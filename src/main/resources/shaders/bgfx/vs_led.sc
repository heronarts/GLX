$input a_position, a_color0, a_texcoord0
$output v_texcoord0, v_texcoord1, v_color0

/*
 * Copyright 2011-2019 Branimir Karadzic. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx#license-bsd-2-clause
 */

#include "../common/common.sh"

uniform vec4 u_dimensions;
#define u_contrast u_dimensions.x
#define u_feather u_dimensions.y
#define u_aspectRatio u_dimensions.z
#define u_pointScale u_dimensions.w

uniform vec4 u_sparkle;
#define u_sparkleAmount u_sparkle.x
#define u_sparkleCurve u_sparkle.y
#define u_sparkleRotate u_sparkle.z
#define u_sparkleOffset u_sparkle.w

void main()
{
  
  // Apply a reverse-gamma curve to the brightest of the color components
  float maxC = max(max(a_color0.r, a_color0.g), a_color0.b);
  float adjusted = maxC;
  float ratio = 1.0f;
  if (maxC > 0.0f) {
    // Use this ratio on all color components (minimizes hue distortion vs. piecewise-gamma)
    adjusted = 1.0f - pow(1.0f - maxC, u_contrast);
    ratio = adjusted / maxC;
  }
  v_color0 = vec4(a_color0.rgb * ratio, a_color0.a);
  
  gl_Position =
    mul(u_modelViewProj, vec4(a_position, 1.0f)) +
    mul(
      u_pointScale * mix(1.0f, clamp(length(v_color0.rgb), 0.0f, 1.0f), u_feather),
      vec4(vec2(1.0f, u_aspectRatio) * (a_texcoord0 - vec2(0.5f, 0.5f)), 0.0f, 0.0f)
    );  

  v_texcoord0 = a_texcoord0;
  
  maxC = max(max(v_color0.r, v_color0.g), v_color0.b);
  float angle = u_sparkleOffset + maxC * u_sparkleRotate;
  float sinA = sin(angle);
  float cosA = cos(angle);
  float sparkleSize = pow(maxC, u_sparkleCurve);
  float stretch = mix(3.0f, 1.0f, sparkleSize);
  
  v_texcoord1 = vec3(
    0.5f + stretch * (a_texcoord0.x-0.5f) * cosA - stretch * (a_texcoord0.y - 0.5f) * sinA,
    0.5f + stretch * (a_texcoord0.x-0.5f) * sinA + stretch * (a_texcoord0.y - 0.5f) * cosA,
    u_sparkleAmount * sparkleSize
  );  

}
