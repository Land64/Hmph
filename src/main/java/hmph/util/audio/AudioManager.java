package hmph.util.audio;

import hmph.util.debug.LoggerHelper;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AudioManager {
    private Map<String, Clip> sounds = new HashMap<>();
    private float masterVolume = 1.0f;
    private boolean initialized = false;

    /**
     * Initialize audio system
     */
    public boolean initialize() {
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

    /**
     * Load sound from WAV or MP3 file
     */
    public boolean loadSound(String name, String path) {
        if (!initialized) return false;

        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream==null) {
                LoggerHelper.betterPrint("Sound file not found: " + path, LoggerHelper.LogType.ERROR);
                return false;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new BufferedInputStream(stream));
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);

            sounds.put(name, clip);
            LoggerHelper.betterPrint("Loaded sound: " + name, LoggerHelper.LogType.INFO);
            return true;
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error loading sound " + path + ": " + e.getMessage(), LoggerHelper.LogType.ERROR);
            return false;
        }
    }

    /**
     * Play sound
     */
    public void playSound(String name) {
        playSound(name, 1.0f, false);
        LoggerHelper.betterPrint(name + " been playedd..", LoggerHelper.LogType.DEBUG);
    }

    /**
     * Play sound with volume
     */
    public void playSound(String name, float volume, boolean loop) {
        if (!initialized) return;

        Clip clip = sounds.get(name);
        if (clip==null) {
            LoggerHelper.betterPrint("Sound not found: " + name, LoggerHelper.LogType.ERROR);
            return;
        }

        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float gain = 20f * (float) Math.log10(volume * masterVolume);
                gain = Math.max(gainControl.getMinimum(), Math.min(gain, gainControl.getMaximum()));
                gainControl.setValue(gain);
            }

            clip.setFramePosition(0);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
        } catch (Exception e) {
            LoggerHelper.betterPrint("Error playing sound " + name + ": " + e.getMessage(), LoggerHelper.LogType.ERROR);
        }
    }

    /**
     * Stop sound
     */
    public void stopSound(String name) {
        if (!initialized) return;

        Clip clip = sounds.get(name);
        if (clip!=null) {
            clip.stop();
            LoggerHelper.betterPrint(name + " been stopped..", LoggerHelper.LogType.DEBUG);
        }
    }

    /**
     * Stop all sounds
     */
    public void stopAllSounds() {
        if (!initialized) return;

        for (Clip clip : sounds.values()) {
            clip.stop();
        }
    }

    /**
     * Check if sound is playing
     */
    public boolean isPlaying(String name) {
        if (!initialized) return false;

        Clip clip = sounds.get(name);
        return clip!=null && clip.isRunning();
    }

    /**
     * Set master volume
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    /**
     * Get master volume
     */
    public float getMasterVolume() {
        return masterVolume;
    }

    /**
     * Cleanup audio resources
     */
    public void cleanup() {
        if (!initialized) return;

        stopAllSounds();

        for (Clip clip : sounds.values()) {
            clip.close();
        }
        sounds.clear();

        initialized = false;
        LoggerHelper.betterPrint("Audio system cleaned up", LoggerHelper.LogType.INFO);
    }
}