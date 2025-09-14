package hmph.rendering;

import hmph.rendering.world.Direction;
import hmph.util.debug.LoggerHelper;

import java.io.*;
import java.util.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.*;
import java.util.stream.Collectors;

public class BlockRegistry {
    private final Map<String, BlockData> blocks = new HashMap<>();
    private final Map<Integer, String> idToName = new HashMap<>();
    private int nextID = 1;

    public static class BlockData {
        public final String type;
        public final Map<Direction, String> faceTextures;

        public BlockData(String type, Map<Direction, String> faceTextures) {
            this.type = type;
            this.faceTextures = faceTextures;
        }
    }

    public void registerBlock(String name, String type, Map<Direction, String> textures) {
        blocks.put(name, new BlockData(type, textures));
        idToName.put(nextID, name);
        nextID++;
    }

    public String getTexture(String name, Direction dir) {
        BlockData data = blocks.get(name);
        if (data == null) return null;
        return data.faceTextures.get(dir);
    }

    public String getType(String name) {
        BlockData data = blocks.get(name);
        if (data == null) return null;
        return data.type;
    }

    public String getNameFromID(int id) {
        if (id == 0) return null; // Air block
        return idToName.get(id);
    }

    public int getIDFromName(String name) {
        for (Map.Entry<Integer, String> entry : idToName.entrySet()) {
            if (entry.getValue().equals(name)) {
                return entry.getKey();
            }
        }
        return 0; // Not found, return air
    }

    public void loadBlocks(String folderPath) {
        try {
            List<String> resources = listResourceFiles(folderPath);
            for (String res : resources) {
                loadBlockFile(folderPath + "/" + res);
            }
        } catch (Exception e) {
            System.err.println("Error loading blocks from " + folderPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadBlockFile(String resourcePath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("Could not find resource: " + resourcePath);
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String json = br.lines().map(String::trim).collect(Collectors.joining());
                String globalName = parseValue(json, "global");
                if (globalName == null) {
                    System.err.println("No 'global' name found in " + resourcePath);
                    return;
                }

                Map<String, String> texturesMap = parseTextures(json);

                Map<Direction, String> faceTextures = new EnumMap<>(Direction.class);
                if (texturesMap.containsKey("all")) {
                    for (Direction dir : Direction.values()) {
                        faceTextures.put(dir, texturesMap.get("all"));
                    }
                } else {
                    String rest = texturesMap.getOrDefault("rest", null);
                    for (Direction dir : Direction.values()) {
                        String key = dir.name().toLowerCase();
                        if (texturesMap.containsKey(key)) {
                            faceTextures.put(dir, texturesMap.get(key));
                        } else if (rest != null) {
                            faceTextures.put(dir, rest);
                        }
                    }
                }
                registerBlock(globalName, globalName, faceTextures);
                //LoggerHelper.betterPrint("Registered block: " + globalName + " with ID: " + (nextID - 1), LoggerHelper.LogType.RENDERING);
            }
        } catch (Exception e) {
            System.err.println("Error loading block file " + resourcePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String parseValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        return matchRegex(json, pattern);
    }

    private Map<String, String> parseTextures(String json) {
        Map<String, String> map = new HashMap<>();
        String texturesJson = matchRegex(json, "\"textures\"\\s*:\\s*\\{([^}]*)\\}");
        if (texturesJson == null) return map;

        String[] entries = texturesJson.split(",");
        for (String entry : entries) {
            String[] kv = entry.split(":");
            if (kv.length != 2) continue;
            String key = kv[0].replaceAll("\"", "").trim();
            String value = kv[1].replaceAll("\"", "").trim();
            map.put(key, value);
        }
        return map;
    }

    public BlockData get(String name) {
        return blocks.get(name);
    }

    private String matchRegex(String input, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(input);
        return m.find() ? m.group(1) : null;
    }

    private List<String> listResourceFiles(String path) throws IOException, URISyntaxException {
        URL dirURL = getClass().getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            return Arrays.asList(new File(dirURL.toURI()).list());
        }
        if (dirURL != null && dirURL.getProtocol().equals("jar")) {
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
            try (JarFile jar = new JarFile(jarPath)) {
                return jar.stream()
                        .map(JarEntry::getName)
                        .filter(name -> name.startsWith(path + "/") && name.endsWith(".json"))
                        .map(name -> name.substring(name.lastIndexOf("/") + 1))
                        .collect(Collectors.toList());
            }
        }
        throw new UnsupportedOperationException("Cannot list files for path: " + path);
    }
}