package hmph.rendering.shaders;

import static org.lwjgl.opengl.GL20.*;
import java.util.HashMap;
import java.util.Map;
import hmph.math.Matrix4f;
import hmph.math.Vector3f;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

public class ShaderProgram {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private Map<String, Integer> uniforms;

    public ShaderProgram() throws Exception {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new Exception("Could not create Shader");
        }
        uniforms = new HashMap<>();
    }

    public void createVertexShader(String shaderCode) throws Exception {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }

    protected int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling Shader code: " + glGetShaderInfoLog(shaderId, 1024));
        }

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    public void link() throws Exception {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new Exception("Error linking Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }

    public void createUniform(String uniformName) throws Exception {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            throw new Exception("Could not find uniform:" + uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }

    public boolean createUniformSafe(String uniformName) {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            System.err.println("Warning: Could not find uniform: " + uniformName);
            return false;
        }
        uniforms.put(uniformName, uniformLocation);
        return true;
    }

    public boolean hasUniform(String uniformName) {
        return uniforms.containsKey(uniformName);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found in shader");
            return;
        }
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        value.get(fb);
        glUniformMatrix4fv(location, false, fb);
    }

    public void setUniform(String uniformName, Vector3f value) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found in shader");
            return;
        }
        glUniform3f(location, value.x, value.y, value.z);
    }


    public void setUniform(String uniformName, float value) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found in shader");
            return;
        }
        glUniform1f(location, value);
    }


    public void setUniform(String uniformName, int value) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found in shader");
            return;
        }
        glUniform1i(location, value);
    }


    public void setUniform3f(String uniformName, float x, float y, float z) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found in shader");
            return;
        }
        glUniform3f(location, x, y, z);
    }


    public void setUniform4f(String uniformName, float x, float y, float z, float w) {
        Integer location = uniforms.get(uniformName);
        if (location == null) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found in shader");
            return;
        }
        glUniform4f(location, x, y, z, w);
    }
    public boolean setUniformSafe(String uniformName, Matrix4f value) {
        if (!hasUniform(uniformName)) {
            return false;
        }
        setUniform(uniformName, value);
        return true;
    }


    public boolean setUniformSafe(String uniformName, Vector3f value) {
        if (!hasUniform(uniformName)) {
            return false;
        }
        setUniform(uniformName, value);
        return true;
    }

    public boolean setUniformSafe(String uniformName, float value) {
        if (!hasUniform(uniformName)) {
            return false;
        }
        setUniform(uniformName, value);
        return true;
    }

    public boolean setUniformSafe(String uniformName, int value) {
        if (!hasUniform(uniformName)) {
            return false;
        }
        setUniform(uniformName, value);
        return true;
    }

    public java.util.Set<String> getUniformNames() {
        return uniforms.keySet();
    }

    public int getProgramId() {
        return programId;
    }

    public void debugPrint() {
        System.out.println("ShaderProgram ID: " + programId);
        System.out.println("Uniforms (" + uniforms.size() + "):");
        for (Map.Entry<String, Integer> entry : uniforms.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> location " + entry.getValue());
        }
    }
}