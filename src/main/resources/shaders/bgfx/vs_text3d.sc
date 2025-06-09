$input a_position, a_texcoord0
$output v_texcoord0

/*
 * Copyright 2011-2023 Branimir Karadzic. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx/blob/master/LICENSE
 */

#include "../common/common.sh"

uniform vec4 u_textPosition;
#define u_textOrientation u_textPosition.w

uniform vec4 u_textMetrics;
#define u_textSize u_textMetrics.x
#define u_textScale u_textMetrics.y
#define u_aspectRatio u_textMetrics.z
#define u_invHeight u_textMetrics.w

uniform vec4 u_textOffset;

void main()
{
  vec4 t_base = mul(u_modelViewProj, vec4(u_textPosition.xyz, 1.0));
  float textSize = u_textSize;
  if (u_textScale == 1.0) {
    // Fixed pixel-based font size
    textSize *= t_base.w * u_invHeight;
  }
  if (u_textOrientation == 1.0) {
    // Camera orientation
    gl_Position = t_base + textSize * vec4(
      (a_position.x + u_textOffset.x) / u_aspectRatio,
      a_position.y + u_textOffset.y,
      0.0,
      0.0
    );
  } else {
    // World orientation
    vec3 t_offset = textSize * (a_position + vec3(u_textOffset.x, u_textOffset.y, 0.0));
  	gl_Position = mul(u_modelViewProj, vec4(u_textPosition.xyz + t_offset, 1.0));
  }
  v_texcoord0 = a_texcoord0;
}
