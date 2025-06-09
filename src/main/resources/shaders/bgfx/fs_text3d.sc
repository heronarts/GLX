$input v_texcoord0

/*
 * Copyright 2011-2023 Branimir Karadzic. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx/blob/master/LICENSE
 */

#include "../common/common.sh"

uniform vec4 u_textColor;
uniform vec4 u_backgroundColor;

SAMPLER2D(s_font, 0);

void main()
{
  gl_FragColor = mix(u_backgroundColor, u_textColor, texture2D(s_font, v_texcoord0.xy).a);
  if (gl_FragColor.a <= 0) {
    discard;
  }
}
