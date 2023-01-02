#version 330 core
layout (location=0) in vec2 aPos;
layout (location=2) in vec2 aTexCoords;

uniform mat4 uProjection;
uniform mat4 uView;

out vec4 fColor;
out vec2 fTexCoords;

void main()
{
    fTexCoords = aTexCoords;
    gl_Position = uProjection * uView * vec4(aPos, 1.0, 1.0);
}

//|

#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 fTexCoords;

out vec4 color;

void main()
{
    vec4 textureColor = texture(TEX_SAMPLER, fTexCoords);
    float alpha = textureColor.w;
    if (alpha > 0) color = texture(TEX_SAMPLER, fTexCoords);
}