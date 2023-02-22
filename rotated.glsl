#version 330 core
layout (location=0) in vec2 aPos;
layout (location=1) in vec2 aPivot;
layout (location=2) in float aRotation;
layout (location=3) in float aTexPosition;

uniform mat4 uProjection;
uniform mat4 uView;

out vec2 fTexPosition;

void main()
{
    float cosa = cos(aRotation);
    float sina = sin(aRotation);
    vec2 position = aPos;
    position -= aPivot;
    vec2 pos = vec2(
        cosa * position.x - sina * position.y,
        cosa * position.y + sina * position.x
    ) + aPivot;

    fTexPosition = vec2(mod(aTexPosition, 512.0), aTexPosition / 512.0) / 512.0;
//    fTexPosition = aTexPosition;
    gl_Position = uProjection * uView * vec4(pos, 1.0, 1.0);
}

//|

#version 330 core

uniform sampler2D TEX_SAMPLER;

in vec2 fTexPosition;

out vec4 color;

void main()
{
    color = texture(TEX_SAMPLER, fTexPosition);
}