package hmph.rendering.shaders;

import java.util.HashMap;
import java.util.Map;

public class ShaderManager {
    private Map<String, ShaderProgram> shaders;

    public ShaderManager() {
        shaders = new HashMap<>();
    }

    public void loadShader(String name, String vertexSource, String fragmentSource) throws Exception {
        ShaderProgram shader = new ShaderProgram();
        shader.createVertexShader(vertexSource);
        shader.createFragmentShader(fragmentSource);
        shader.link();
        shader.bind();
        if (name.equals("3d")) {
            shader.createUniform("model");
            shader.createUniform("view");
            shader.createUniform("projection");
            shader.createUniform("texture1");
            shader.createUniform("color");
        } else if (name.equals("textured")) {
            shader.createUniform("model");
            shader.createUniform("projection");
            shader.createUniform("color");
            shader.createUniform("texture1");
        } else if (name.equals("text")) {
            shader.createUniform("projection");
            shader.createUniform("textColor");
            shader.createUniform("textTexture");
        }
        shader.unbind();
        shaders.put(name, shader);
    }

    public ShaderProgram getShader(String name) {
        return shaders.get(name);
    }

    public void cleanup() {
        for (ShaderProgram shader : shaders.values()) {
            shader.cleanup();
        }
        shaders.clear();
    }

    public void loadDefaultShaders() throws Exception {
        loadShader("basic", BASIC_VERTEX_SHADER, BASIC_FRAGMENT_SHADER);
        loadShader("3d", VERTEX_3D_SHADER, FRAGMENT_3D_SHADER);
        loadShader("textured", TEXTURED_VERTEX_SHADER, TEXTURED_FRAGMENT_SHADER);
        loadShader("text", TEXT_VERTEX_SHADER, TEXT_FRAGMENT_SHADER);
    }
    private static final String BASIC_VERTEX_SHADER = """
        #version 330 core
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec3 aColor;
        
        out vec3 vertexColor;
        
        void main() {
            gl_Position = vec4(aPos, 1.0);
            vertexColor = aColor;
        }
        """;
    private static final String BASIC_FRAGMENT_SHADER = """
        #version 330 core
        in vec3 vertexColor;
        out vec4 FragColor;
        
        void main() {
            FragColor = vec4(vertexColor, 1.0);
        }
        """;
    private static final String VERTEX_3D_SHADER = """
        #version 330 core
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec2 aTexCoord;
    
        uniform mat4 model;
        uniform mat4 view;
        uniform mat4 projection;
    
        out vec2 TexCoord;
        out vec3 worldPos;
    
        void main() {
            worldPos = vec3(model * vec4(aPos, 1.0));
            gl_Position = projection * view * vec4(worldPos, 1.0);
            TexCoord = aTexCoord;
        }
    """;

    private static final String FRAGMENT_3D_SHADER = """
        #version 330 core
        in vec2 TexCoord;
        in vec3 worldPos;
        out vec4 FragColor;
    
        uniform sampler2D texture1;
        uniform vec3 color;
    
        void main() {
            vec4 texColor = texture(texture1, TexCoord);
            if (texColor.a < 0.1) {
                discard; //Transparency!
            }
        FragColor = texColor * vec4(color, 1.0);
        }
    """;

    private static final String TEXTURED_VERTEX_SHADER = """
        #version 330 core
        layout (location = 0) in vec2 aPos;
        layout (location = 1) in vec2 aTexCoord;
        
        uniform mat4 model;
        uniform mat4 projection;
        
        out vec2 TexCoord;
        
        void main() {
            gl_Position = projection * model * vec4(aPos, 0.0, 1.0);
            TexCoord = aTexCoord;
        }
        """;

    private static final String TEXTURED_FRAGMENT_SHADER = """
        #version 330 core
        in vec2 TexCoord;
        out vec4 FragColor;
        
        uniform sampler2D texture1;
        uniform vec3 color;
        
        void main() {
            vec4 texColor = texture(texture1, TexCoord);
            FragColor = texColor * vec4(color, 1.0);
        }
        """;

    private static final String TEXT_VERTEX_SHADER = """
        #version 330 core
        layout(location = 0) in vec2 aPos;
        layout(location = 1) in vec2 aTex;
        out vec2 TexCoords;
        uniform mat4 projection;
        void main() {
            gl_Position = projection * vec4(aPos, 0.0, 1.0);
            TexCoords = aTex;
        }
    """;

    private static final String TEXT_FRAGMENT_SHADER = """
        #version 330 core
        in vec2 TexCoords;
        out vec4 FragColor;
        uniform sampler2D textTexture;
        uniform vec3 textColor;
        void main() {
            float alpha = texture(textTexture, TexCoords).r;
            FragColor = vec4(textColor, alpha);
        }
    """;
}