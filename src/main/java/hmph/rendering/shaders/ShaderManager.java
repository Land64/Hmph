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
            shader.createUniform("lightDirection");
            shader.createUniform("lightColor");
            shader.createUniform("ambientStrength");
            shader.createUniform("ambientColor");
        } else if (name.equals("textured")) {
            shader.createUniform("model");
            shader.createUniform("projection");
            shader.createUniform("color");
            shader.createUniform("texture1");
        } else if (name.equals("text")) {
            shader.createUniform("projection");
            shader.createUniform("textColor");
            shader.createUniform("textTexture");
        } else if (name.equals("skybox")) {
            shader.createUniformSafe("projection");
            shader.createUniformSafe("view");
            shader.createUniformSafe("timeOfDay");
        }



        shader.unbind();
        shaders.put(name, shader);
    }

    public ShaderProgram getShader(String name) {
        return shaders.get(name);
    }

    public void addShader(String name, ShaderProgram shader) {
        shaders.put(name, shader);
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

    private static final String BASIC_VERTEX_SHADER =
            "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "layout (location = 1) in vec3 aColor;\n" +
            "\n" +
            "out vec3 vertexColor;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = vec4(aPos, 1.0);\n" +
            "    vertexColor = aColor;\n" +
            "}\n";

    private static final String BASIC_FRAGMENT_SHADER =
            "#version 330 core\n" +
            "in vec3 vertexColor;\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "void main() {\n" +
            "    FragColor = vec4(vertexColor, 1.0);\n" +
            "}\n";

    private static final String VERTEX_3D_SHADER =
            "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "layout (location = 1) in vec2 aTexCoord;\n" +
            "layout (location = 2) in vec3 aNormal;\n" +
            "\n" +
            "uniform mat4 model;\n" +
            "uniform mat4 view;\n" +
            "uniform mat4 projection;\n" +
            "\n" +
            "out vec2 TexCoord;\n" +
            "out vec3 worldPos;\n" +
            "out vec3 Normal;\n" +
            "\n" +
            "void main() {\n" +
            "    worldPos = vec3(model * vec4(aPos, 1.0));\n" +
            "    gl_Position = projection * view * vec4(worldPos, 1.0);\n" +
            "    TexCoord = aTexCoord;\n" +
            "    Normal = mat3(transpose(inverse(model))) * aNormal;\n" +
            "}\n";

    private static final String FRAGMENT_3D_SHADER =
            "#version 330 core\n" +
            "in vec2 TexCoord;\n" +
            "in vec3 worldPos;\n" +
            "in vec3 Normal;\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "uniform sampler2D texture1;\n" +
            "uniform vec3 color;\n" +
            "uniform vec3 lightDirection;\n" +
            "uniform vec3 lightColor;\n" +
            "uniform float ambientStrength;\n" +
            "uniform vec3 ambientColor;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 texColor = texture(texture1, TexCoord);\n" +
            "    if (texColor.a < 0.1) {\n" +
            "        discard;\n" +
            "    }\n" +
            "    \n" +
            "    vec3 norm = normalize(Normal);\n" +
            "    vec3 lightDir = normalize(-lightDirection);\n" +
            "    float diff = max(dot(norm, lightDir), 0.0);\n" +
            "    \n" +
            "    vec3 ambient = ambientStrength * ambientColor;\n" +
            "    vec3 diffuse = diff * lightColor;\n" +
            "    vec3 lighting = ambient + diffuse;\n" +
            "    \n" +
            "    lighting = max(lighting, vec3(0.05, 0.05, 0.1));\n" +
            "    \n" +
            "    FragColor = texColor * vec4(color * lighting, 1.0);\n" +
            "}\n";

    private static final String TEXTURED_VERTEX_SHADER =
            "#version 330 core\n" +
            "layout (location = 0) in vec2 aPos;\n" +
            "layout (location = 1) in vec2 aTexCoord;\n" +
            "\n" +
            "uniform mat4 model;\n" +
            "uniform mat4 projection;\n" +
            "\n" +
            "out vec2 TexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = projection * model * vec4(aPos, 0.0, 1.0);\n" +
            "    TexCoord = aTexCoord;\n" +
            "}\n";

    private static final String TEXTURED_FRAGMENT_SHADER =
            "#version 330 core\n" +
            "in vec2 TexCoord;\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "uniform sampler2D texture1;\n" +
            "uniform vec3 color;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 texColor = texture(texture1, TexCoord);\n" +
            "    FragColor = texColor * vec4(color, 1.0);\n" +
            "}\n";

    private static final String TEXT_VERTEX_SHADER =
            "#version 330 core\n" +
            "layout(location = 0) in vec2 aPos;\n" +
            "layout(location = 1) in vec2 aTex;\n" +
            "out vec2 TexCoords;\n" +
            "uniform mat4 projection;\n" +
            "void main() {\n" +
            "    gl_Position = projection * vec4(aPos, 0.0, 1.0);\n" +
            "    TexCoords = aTex;\n" +
            "}\n";

    private static final String TEXT_FRAGMENT_SHADER =
            "#version 330 core\n" +
            "in vec2 TexCoords;\n" +
            "out vec4 FragColor;\n" +
            "uniform sampler2D textTexture;\n" +
            "uniform vec3 textColor;\n" +
            "void main() {\n" +
            "    float alpha = texture(textTexture, TexCoords).r;\n" +
            "    FragColor = vec4(textColor, alpha);\n" +
            "}\n";
}