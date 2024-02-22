$input v_color0, v_texcoord0, v_texcoord1
  
/*
 * Copyright 2011-2019 Branimir Karadzic. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx#license-bsd-2-clause
 */

#include "../common/common.sh"

SAMPLER2D(s_texColor, 0);
SAMPLER2D(s_texSparkle, 1);

void main()
{
  vec4 sparkleSample = texture2D(s_texSparkle, v_texcoord1.xy);
  sparkleSample.a *= v_texcoord1.z;
  vec4 sparkleColor = vec4(sparkleSample.a * sparkleSample.bgr, sparkleSample.a);  
  
  gl_FragColor = v_color0.bgra * min(
  	vec4(1.0f, 1.0f, 1.0f, 1.0f),
    texture2D(s_texColor, v_texcoord0) + sparkleColor
  );
  
  // Check alpha ref after masking by texture alpha
  if (gl_FragColor.a <= u_alphaRef) {
    discard;
  }
}
