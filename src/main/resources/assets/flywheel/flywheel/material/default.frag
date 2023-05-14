#include "flywheel:api/fragment.glsl"
#include "flywheel:util/fog.glsl"

void flw_materialFragment() {
}

vec4 flw_fogFilter(vec4 color) {
    return linear_fog(color, flw_distance, flywheel.fogRange.x, flywheel.fogRange.y, flywheel.fogColor);
}
