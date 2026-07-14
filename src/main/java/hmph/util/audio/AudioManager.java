package hmph.util.audio;

import hmph.util.debug.LoggerHelper;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioManager {
    public static final float DEFAULT_MASTER_VOLUME = 0.12f;

    private final Map<String, Clip> sounds = new HashMap<>();
    private final Map<Clip, Float> clipVolumes = new HashMap<>();
    private final Map<String, Playlist> playlists = new HashMap<>();
    private float masterVolume = DEFAULT_MASTER_VOLUME;
    private boolean initialized;
    private Playlist currentPlaylist;
    private Clip playlistClip;
    private LineListener playlistListener;

    public boolean initialize() {
        if (initialized) return true;
        try {
            AudioSystem.getMixer(null);
            initialized = true;
            LoggerHelper.betterPrint("Audio system initialized", LoggerHelper.LogType.INFO);
            return true;
        } catch (Exception e) {
            LoggerHelper.betterPrint("Failed to initialize audio: " + e.getMessage(), LoggerHelper.LogType.ERROR);
            return false;
        }
    }

    public boolean loadSound(String name, String path) {
        if (!initialized) return false;

        InputStream resource = getClass().getClassLoader().getResourceAsStream(path);
        if (resource == null) {
            LoggerHelper.betterPrint("Sound file not found: " + path, LoggerHelper.LogType.ERROR);
            return false;
        }

        try (resource;
             BufferedInputStream buffered = new BufferedInputStream(resource);
             AudioInputStream audioStream = AudioSystem.getAudioInputStream(buffered)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clipVolumes.put(clip, 1.0f);
            applyGain(clip, 1.0f);

            Clip previous = sounds.put(name, clip);
            if (previous != null) {
                clipVolumes.remove(previous);
                previous.stop();
                previous.close();
            }
            LoggerHelper.betterPrint("Loaded sound: " + name, LoggerHelper.LogType.INFO);
            return true;
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error loading sound " + path + ": " + e.getMessage(), LoggerHelper.LogType.ERROR);
            return false;
        }
    }

    public void playSound(String name) {
        playSound(name, 1.0f, false);
    }

    public synchronized void playSound(String name, float volume, boolean loop) {
        if (!initialized) return;
        Clip clip = sounds.get(name);
        if (clip == null) {
            LoggerHelper.betterPrint("Sound not found: " + name, LoggerHelper.LogType.ERROR);
            return;
        }

        try {
            float clampedVolume = clamp01(volume);
            clipVolumes.put(clip, clampedVolume);
            applyGain(clip, clampedVolume);
            clip.stop();
            clip.setFramePosition(0);
            if (loop) clip.loop(Clip.LOOP_CONTINUOUSLY);
            else clip.start();
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error playing sound " + name + ": " + e.getMessage(), LoggerHelper.LogType.ERROR);
        }
    }

    public synchronized void stopSound(String name) {
        Clip clip = sounds.get(name);
        if (clip != null) clip.stop();
    }

    public synchronized void stopAllSounds() {
        stopPlaylist();
        for (Clip clip : sounds.values()) clip.stop();
    }

    public boolean isPlaying(String name) {
        Clip clip = sounds.get(name);
        return initialized && clip != null && clip.isRunning();
    }

    public synchronized void setMasterVolume(float volume) {
        masterVolume = clamp01(volume);
        for (Map.Entry<Clip, Float> entry : clipVolumes.entrySet()) {
            applyGain(entry.getKey(), entry.getValue());
        }
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public synchronized void cleanup() {
        if (!initialized) return;
        stopAllSounds();
        for (Clip clip : sounds.values()) clip.close();
        sounds.clear();
        clipVolumes.clear();
        playlists.clear();
        initialized = false;
        LoggerHelper.betterPrint("Audio system cleaned up", LoggerHelper.LogType.INFO);
    }

    public void createPlaylist(String name, List<String> soundNames, boolean loop) {
        playlists.put(name, new Playlist(soundNames, loop));
    }

    public synchronized void playPlaylist(String name) {
        Playlist playlist = playlists.get(name);
        if (playlist == null || playlist.sounds.isEmpty()) {
            LoggerHelper.betterPrint("Playlist not found or empty: " + name, LoggerHelper.LogType.ERROR);
            return;
        }
        stopPlaylist();
        playlist.reset();
        currentPlaylist = playlist;
        playNextInPlaylist();
    }

    public synchronized void stopPlaylist() {
        currentPlaylist = null;
        detachPlaylistListener();
        if (playlistClip != null) {
            playlistClip.stop();
            playlistClip = null;
        }
    }

    private synchronized void playNextInPlaylist() {
        Playlist playlist = currentPlaylist;
        if (playlist == null) return;

        String nextSound = playlist.getNextSound();
        if (nextSound == null) {
            currentPlaylist = null;
            return;
        }

        Clip clip = sounds.get(nextSound);
        if (clip == null) {
            LoggerHelper.betterPrint("Sound in playlist not found: " + nextSound, LoggerHelper.LogType.ERROR);
            playNextInPlaylist();
            return;
        }

        detachPlaylistListener();
        playlistClip = clip;
        clipVolumes.put(clip, 1.0f);
        applyGain(clip, 1.0f);
        clip.stop();
        clip.setFramePosition(0);

        playlistListener = new LineListener() {
            @Override
            public void update(LineEvent event) {
                if (event.getType() != LineEvent.Type.STOP) return;
                synchronized (AudioManager.this) {
                    if (playlistListener != this || currentPlaylist != playlist) return;
                    boolean finished = clip.getFramePosition() >= clip.getFrameLength();
                    if (!finished) return;
                    detachPlaylistListener();
                    playlistClip = null;
                    playNextInPlaylist();
                }
            }
        };
        clip.addLineListener(playlistListener);
        clip.start();
    }

    private void detachPlaylistListener() {
        if (playlistClip != null && playlistListener != null) {
            playlistClip.removeLineListener(playlistListener);
        }
        playlistListener = null;
    }

    private void applyGain(Clip clip, float soundVolume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float linear = clamp01(soundVolume) * masterVolume;
        float decibels = linear <= 0.0001f ? gain.getMinimum() : 20f * (float) Math.log10(linear);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(decibels, gain.getMaximum())));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public static final class Playlist {
        private final List<String> sounds;
        private final boolean loop;
        private int index;

        private Playlist(List<String> sounds, boolean loop) {
            this.sounds = new ArrayList<>(sounds);
            this.loop = loop;
        }

        private void reset() {
            index = 0;
        }

        private String getNextSound() {
            if (sounds.isEmpty()) return null;
            if (index >= sounds.size()) {
                if (!loop) return null;
                index = 0;
            }
            return sounds.get(index++);
        }
    }
}
