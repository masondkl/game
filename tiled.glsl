#version 330 core
layout (location=0) in vec2 aPosition;
layout (location=1) in vec2 aStart;
//layout (location=2) in float aScale;
layout (location=2) in float aTexStart;
//layout (location=4) in float aTexScale;

uniform mat4 uProjection;
uniform mat4 uView;

out vec2 fPosition;
out vec2 fStart;
//out float fScale;
out vec2 fTexStart;
//out float fTexScale;

void main()
{
    float sheetSize = 512.0;
    fPosition = aPosition;
    fStart = aStart;
//    fScale = aScale;
    fTexStart = vec2(mod(aTexStart, sheetSize), aTexStart / sheetSize) / sheetSize;
//    fTexScale = aTexScale;
    gl_Position = uProjection * uView * vec4(aPosition, 1.0, 1.0);
}

//|

#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 fPosition;
in vec2 fStart;
//in float fScale;
in vec2 fTexStart;
//in float fTexScale;

out vec4 color;

void main()
{
    float fScale = 32.0;
    float fTexScale = 8.0 / 512.0;
    color = texture(TEX_SAMPLER, fTexStart +
            fract((fPosition - fStart) / fScale) * vec2(fTexScale, -fTexScale)
    );
}