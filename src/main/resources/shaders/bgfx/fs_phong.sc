$input v_normal, v_pos

/*
 * Copyright 2011-2023 Branimir Karadzic. All rights reserved.
 * License: https://github.com/bkaradzic/bgfx/blob/master/LICENSE
 */

#include "../common/common.sh"

uniform vec4 u_lightColor;
uniform vec4 u_lightDirection;
uniform vec4 u_lighting;
#define u_ambient u_lighting.x
#define u_diffuse u_lighting.y
#define u_specular u_lighting.z
#define u_shininess u_lighting.w

uniform vec4 u_eyePosition;

uniform vec4 u_objectColor;

void main()
{
  vec3 lightColor = u_lightColor.w * u_lightColor.xyz;
  
  // Ambient lighting  
  vec3 ambient = u_ambient * lightColor;    

  // Diffuse lighting
  vec3 norm = normalize(v_normal);
  vec3 lightDir = u_lightDirection.xyz;
  float diff = max(dot(norm, -lightDir), 0.0);
  vec3 diffuse = u_diffuse * diff * lightColor;

  // Specular highlights
  vec3 reflectDir = reflect(lightDir, norm);  
  vec3 viewDir = normalize(u_eyePosition.xyz - v_pos);

  // Phong
  // float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_shininess);

  // Blinn-Phong
  vec3 halfDir = normalize(-lightDir + viewDir);
  float spec = pow(max(dot(norm, halfDir), 0.0), u_shininess);
  
  vec3 specular = u_specular * spec * lightColor; 

  gl_FragColor = vec4(ambient + diffuse + specular, 1.0) * u_objectColor;
}
