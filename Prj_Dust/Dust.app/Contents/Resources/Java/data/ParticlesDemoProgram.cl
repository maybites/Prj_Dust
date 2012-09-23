// Ported to JavaCL/OpenCL4Java (+ added colors) by Olivier Chafik
 
#define REPULSION_FORCE 4.0f
#define CENTER_FORCE2 0.0005f
 
#define PI 3.1416f
 
//#pragma OpenCL cl_khr_byte_addressable_store : enable
 
uchar4 HSVAtoRGBA(float4 hsva)
{
    float h = hsva.x, s = hsva.y, v = hsva.z, a = hsva.w;
    float r, g, b;
 
        int i;
        float f, p, q, t;
        if (s == 0) {
                // achromatic (grey)
                r = g = b = v;
                return (uchar4)(r * 255, g * 255, b * 255, a * 255);
        }
        h /= 60;                        // sector 0 to 5
        i = floor( h );
        f = h - i;                      // factorial part of h
        p = v * ( 1 - s );
        q = v * ( 1 - s * f );
        t = v * ( 1 - s * ( 1 - f ) );
        switch( i ) {
                case 0:
                        r = v;
                        g = t;
                        b = p;
                        break;
                case 1:
                        r = q;
                        g = v;
                        b = p;
                        break;
                case 2:
                        r = p;
                        g = v;
                        b = t;
                        break;
                case 3:
                        r = p;
                        g = q;
                        b = v;
                        break;
                case 4:
                        r = t;
                        g = p;
                        b = v;
                        break;
                default:                // case 5:
                        r = v;
                        g = p;
                        b = q;
                        break;
        }
    return (uchar4)(r * 255, g * 255, b * 255, a * 255);
}
 
__kernel void updateParticle(
        __global float* masses,
        __global float2* velocities,
        //__global Particle* particles,
        __global float4* particles,
        //__global char* pParticles,
        const float2 mousePos,
        const float2 dimensions,
        float massFactor,
        float speedFactor,
        float slowDownFactor,
        float mouseWeight,
        char limitToScreen
) {
	int id = get_global_id(0);
 
        float4 particle = particles[id];
 
        uchar4 color = as_uchar4(particle.x);
 
        float2 position = particle.yz;
    	float2 diff = mousePos - position;
 
        float invDistSQ = 1.0f / dot(diff, diff);
	float2 halfD = dimensions / 2.0f;
        diff *= (halfD).y * invDistSQ;
 
        float mass = massFactor * masses[id];
        float2 velocity = velocities[id];
        velocity -= mass * position * CENTER_FORCE2 - diff * mass * mouseWeight;
        position += speedFactor * velocities[id];
 
        if (limitToScreen) {
            float2 halfDims = dimensions / 2.0f;
            position = clamp(position, -halfDims, halfDims);
        }
 
        float dirDot = cross((float4)(diff, (float2)0), (float4)(velocity, (float2)0)).z;
        float speed = length(velocity);
 
        float f = speed / 4 / mass;
        float hue = (dirDot < 0 ? f : f + 1) / 2;
        hue = clamp(hue, 0.0f, 1.0f) * 360;
 
        float opacity = clamp(0.1f + f, 0.0f, 1.0f);
        float saturation = mass / 2;
        float brightness = 0.6f + opacity * 0.3f;
 
        uchar4 targetColor = HSVAtoRGBA((float4)(hue, saturation, brightness, opacity));
 
        float colorSpeedFactor = min(0.01f * speedFactor, 1.0f), otherColorSpeedFactor = 1 - colorSpeedFactor;
        color = (uchar4)(
            (uchar)(targetColor.x * colorSpeedFactor + color.x * otherColorSpeedFactor),
            (uchar)(targetColor.y * colorSpeedFactor + color.y * otherColorSpeedFactor),
            (uchar)(targetColor.z * colorSpeedFactor + color.z * otherColorSpeedFactor),
            (uchar)(targetColor.w * colorSpeedFactor + color.w * otherColorSpeedFactor)
        );
 
        particle.x = as_float(color);
        particle.yz = position;
 
    	particles[id] = particle;
 
        velocity *= slowDownFactor;
        velocities[id] = velocity;
}