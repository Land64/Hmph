package hmph.rendering;

import hmph.math.Vector3f;

public class LightingSystem {

    public static class LightData {
        public Vector3f direction;
        public Vector3f color;
        public float ambientStrength;
        public Vector3f ambientColor;

        public LightData() {
            direction = new Vector3f();
            color = new Vector3f();
            ambientColor = new Vector3f();
        }
    }

    // Since my teacher really wants me to use comments i will from now on.

    /**
     * Dynamic lighting system baised on given time of day
     * @param timeOfDay 0.0 = midnight, 0.25 = sunrise, 0.5 = noon, 0.75 = sunset, 1.0 = midnight
     * @return LightData containing direction, color, and ambient information
     */
    public static LightData doLighting(float timeOfDay) {
        LightData light = new LightData();

        float sunAngle = timeOfDay * 2.0f * (float)Math.PI;
        float sunHeight = (float)Math.sin(sunAngle);
        float sunX = (float)Math.cos(sunAngle);

        light.direction.x = -sunX * 0.5f;
        light.direction.y = -Math.max(sunHeight, -0.3f);
        light.direction.z = -0.3f;
        light.direction = normalize(light.direction);

        float sunIntensity = Math.max(0.0f, sunHeight + 0.2f);
        sunIntensity = Math.min(1.0f, sunIntensity);

        if (timeOfDay < 0.25f || timeOfDay > 0.75f) {
            float nightFactor = (timeOfDay < 0.25f) ? (0.25f - timeOfDay) / 0.25f : (timeOfDay - 0.75f) / 0.25f;

            light.color.x = 0.4f + nightFactor * 0.3f;
            light.color.y = 0.4f + nightFactor * 0.3f;
            light.color.z = 0.8f + nightFactor * 0.2f;
            light.ambientStrength = 0.15f + nightFactor * 0.1f;
            light.ambientColor.x = 0.1f;
            light.ambientColor.y = 0.1f;
            light.ambientColor.z = 0.3f;

        } else if (timeOfDay >= 0.2f && timeOfDay <= 0.3f) {
            float sunriseFactor = (timeOfDay - 0.2f) / 0.1f;

            light.color.x = 1.0f + sunriseFactor * 0.2f;
            light.color.y = 0.6f + sunriseFactor * 0.3f;
            light.color.z = 0.3f + sunriseFactor * 0.4f;
            light.ambientStrength = 0.2f + sunriseFactor * 0.2f;
            light.ambientColor.x = 0.8f;
            light.ambientColor.y = 0.4f;
            light.ambientColor.z = 0.2f;

        } else if (timeOfDay >= 0.4f && timeOfDay <= 0.6f) {
            light.color.x = 1.0f;
            light.color.y = 1.0f;
            light.color.z = 0.9f;
            light.ambientStrength = 0.4f;
            light.ambientColor.x = 0.6f;
            light.ambientColor.y = 0.7f;
            light.ambientColor.z = 1.0f;

        } else if (timeOfDay >= 0.7f && timeOfDay <= 0.8f) {
            // Sunset (0.7-0.8)
            float sunsetFactor = (timeOfDay - 0.7f) / 0.1f;

            light.color.x = 1.0f + sunsetFactor * 0.3f;
            light.color.y = 0.5f + sunsetFactor * 0.1f;
            light.color.z = 0.2f - sunsetFactor * 0.1f;
            light.ambientStrength = 0.3f - sunsetFactor * 0.1f;
            light.ambientColor.x = 1.0f;
            light.ambientColor.y = 0.6f;
            light.ambientColor.z = 0.3f;

        } else {
            light.color.x = 0.8f;
            light.color.y = 0.8f;
            light.color.z = 0.8f;
            light.ambientStrength = 0.25f;
            light.ambientColor.x = 0.5f;
            light.ambientColor.y = 0.5f;
            light.ambientColor.z = 0.7f;
        }

        light.color.x *= sunIntensity;
        light.color.y *= sunIntensity;
        light.color.z *= sunIntensity;

        return light;
    }

    private static Vector3f normalize(Vector3f v) {
        float length = (float)Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if (length > 0) {
            v.x /= length;
            v.y /= length;
            v.z /= length;
        }
        return v;
    }
}