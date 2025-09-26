# Documentation for Developers

This document tells you how to add your very own ideas to the engine.

---

# 1. Overview

### Hmph is a **3D voxel-based engine** / rendering engine built in *Java* using LWJGL (which is a fancy library for graphics).

The source is under `src/main`, and it’s divided into packages. Just to list some:

* [[Math](https://github.com/Land64/Hmph/tree/master/src/main/java/hmph/math)](https://github.com/Land64/Hmph/tree/master/src/main/java/hmph/math) – Vectors, Matrices, Raycasting
* [[Rendering](https://github.com/Land64/Hmph/tree/master/src/main/java/hmph/rendering)](https://github.com/Land64/Hmph/tree/master/src/main/java/hmph/rendering) – Shaders & World (lighting, blocks, etc.)
* [[Player](https://github.com/Land64/Hmph/tree/master/src/main/java/hmph/player)](https://github.com/Land64/Hmph/tree/master/src/main/java/hmph/player) – Player class, Inventory
* [[GUI](https://github.com/Land64/Hmph/tree/master/src/main/java/hmph/GUI)](https://github.com/Land64/Hmph/tree/master/src/main/java/hmph/GUI) – Rendering handlers, TextObjects, Buttons

That’s just a small percentage of them, but it gives you the vibe.

---

# 2. How to Use Certain Classes (Without Editing Core Code)

These are the main classes you’ll probably use in your mods.

---

## 2.1 – Player ([[ref](https://github.com/Land64/Hmph/blob/master/src/main/java/hmph/player/Player.java)](https://github.com/Land64/Hmph/blob/master/src/main/java/hmph/player/Player.java))

### Public Variables

* PLAYER_HEIGHT
* PLAYER_WIDTH
* GRAVITY
* JUMP_STRENGTH
* MOVE_SPEED

### Useful Methods

* `tryPlaceBlock()` – Places a block where the player is looking. Ignores normal keybind checks, so you can make your own.
* `isPositionInsidePlayer()` – Checks if a block would collide with the player.
* `nextBlock()` & `prevBlock()` – Switch hotbar slots left and right.
* `setHotbarSlot(int slot)` – Directly set a hotbar slot with an integer.
* `getPosition()` – Returns a Vector3f of player position.
* `isOnGround()` – Checks if the player is on the ground.
* `isSprinting()` – Checks if the player is sprinting.
* `getInventory()` – Gets all items the player has.
* `getCurrentLookingAt()` – Gets the block the player is looking at.
* `setPosition(Vector3f pos)` – Set the player’s position using a vector.
* `setPosition(float x, float y, float z)` – Same as above, but uses raw floats.

---

## 2.2 – World & Chunks (in `hmph.rendering.world`)

This is how the engine handles blocks, storage, and loading/unloading.

### Useful Methods

* `getBlock(int x, int y, int z)` – Get the block ID at world coordinates.
* `setBlock(int x, int y, int z, int blockId)` – Set a block at specific coordinates.
* `getChunk(int cx, int cz)` – Get the chunk at chunk coordinates.
* `isLoaded(int cx, int cz)` – Check if a chunk is loaded.
* `regenerateChunk(int cx, int cz)` – Force a chunk to rebuild.

### BlockRegistry

* `registerBlock(int id, Block block)` – Add a custom block type.
* `getBlockById(int id)` – Get a block class by ID.
* `getIdForBlock(Block block)` – Reverse lookup.

---

## 2.3 – Rendering (Shaders, Meshes, Textures)

This is about how things are drawn. If you want custom graphics or wild stuff, this is where.

### ShaderProgram

* `use()` – Activate the shader.
* `setUniform(String name, float value)` – Send numbers or vectors into the shader.
* `compileVertexShader(String src)` – Compile vertex shader code.
* `compileFragmentShader(String src)` – Compile fragment shader code.

### Mesh / Model

* `draw()` – Renders the mesh.
* `rebuild()` – Rebuilds VAO/VBO after changes.
* `addVertex(...)`, `addTriangle(...)` – Build custom geometry.

### TextureAtlas

* `bind()` – Use the texture.
* `getUV(...)` – Get UV coordinates from the atlas.

---

## 2.4 – GUI (Menus, Buttons, HUD)

Mods usually need UI. This package is for making menus or overlays.

### GUIManager

* `pushScreen(Screen screen)` – Show a new screen.
* `popScreen()` – Close it.
* `render()` – Draw all GUI elements.
* `update()` – Handle clicks and input.

### Button

* `onClick()` – Called when clicked.
* `setText(String text)` – Sets button label.
* `setPosition(x, y)` – Position it.
* `setSize(width, height)` – Resize it.

### TextObject

* `drawText(String text, float x, float y, float scale, Color col)` – Draw text.
* `getTextWidth(String text, float scale)` – Get width for alignment.

---

## 2.5 – Math & Raycasting (in `hmph.math`)

Used for collisions, targeting, and procedural stuff.

### Raycasting

* `intersectVoxelGrid(Vector3f origin, Vector3f dir, float maxDist)` – Returns the block hit by a ray.
* `raycastBlocks(...)` – General block picking.

### Bounding Boxes

* `intersects(AABB other)` – Check if two boxes overlap.
* `contains(Vector3f point)` – Check if point is inside.

### Noise

* `PerlinNoise` – For procedural terrain or random heightmaps.

