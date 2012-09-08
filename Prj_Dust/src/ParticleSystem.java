import processing.core.PApplet;
import processing.opengl.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import java.applet.Applet;
import java.util.Random;
 
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLMem.Usage;
import org.bridj.Pointer;
 
import static org.bridj.Pointer.*;
 
public class ParticleSystem extends PApplet{

final int particlesCount = 200000;

// funktioniert auf meiner maschine nicht. scheint zu alte hardware zu sein...

GL2 gl;
PGL pgl;
 
int [] vbo = new int[1];
 
CLContext context;
CLQueue queue;
 
Pointer<Float> velocities;
CLKernel updateParticleKernel;
 
CLBuffer<Float> massesMem, velocitiesMem;
CLBuffer<Byte> interleavedColorAndPositionsMem;
Pointer<Byte> interleavedColorAndPositionsTemp;
 
int elementSize = 4*4;
 
public void setup() {
  size(800, 600, OPENGL);
  background(0);
  randomSeed(millis());
 
  PGraphicsOpenGL pg = (PGraphicsOpenGL) g;
  pgl = pg.beginPGL();
  gl = pgl.gl.getGL().getGL2();
  gl.glClearColor(0, 0, 0, 1);
  gl.glClear(GL.GL_COLOR_BUFFER_BIT);
  gl.glEnable(GL.GL_BLEND);
  gl.glEnable(GL2.GL_POINT_SMOOTH);
  gl.glPointSize(1f);
  initOpenCL();
  pg.endPGL();
}
 
void initOpenCL() {
	this.println(JavaCL.listPlatforms().length);
  context = JavaCL.createContextFromCurrentGL();
  queue = context.createDefaultQueue();
 
  Pointer<Float> masses = allocateFloats(particlesCount).order(context.getByteOrder());
  velocities = allocateFloats(2 * particlesCount).order(context.getByteOrder());
  interleavedColorAndPositionsTemp = allocateBytes(elementSize * particlesCount).order(context.getByteOrder());
 
  Pointer<Float> positionsView = interleavedColorAndPositionsTemp.as(Float.class);
  for (int i = 0; i < particlesCount; i++) {
    masses.set(i, 0.5f + 0.5f * random(1));
    velocities.set(i * 2, (float) (Math.random() - .5f) * 0.2f);
    velocities.set(i * 2 + 1, (float)(Math.random() - .5f) * 0.2f);
    int colorOffset = i * elementSize;
    int posOffset = i * (elementSize / 4) + 1;
    byte r = (byte) 220, g = r, b = r, a = r;
    interleavedColorAndPositionsTemp.set(colorOffset++, r);
    interleavedColorAndPositionsTemp.set(colorOffset++, g);
    interleavedColorAndPositionsTemp.set(colorOffset++, b);
    interleavedColorAndPositionsTemp.set(colorOffset, a);
    float x = (float) ((Math.random() - .5f) * width/2.0), 
    y = (float) ((Math.random() - .5f) * height/2.0);
    positionsView.set(posOffset, (float) x);
    positionsView.set(posOffset + 1, (float) y);
  }
  velocitiesMem = context.createBuffer(Usage.InputOutput, velocities, false);
  massesMem = context.createBuffer(Usage.Input, masses, true);
 
  gl.glGenBuffers(1, vbo, 0);
  gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
  gl.glBufferData(GL.GL_ARRAY_BUFFER, (int) interleavedColorAndPositionsTemp.getValidBytes(), interleavedColorAndPositionsTemp.getByteBuffer(), GL2.GL_DYNAMIC_COPY);
  gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
 
  interleavedColorAndPositionsMem = context.createBufferFromGLBuffer(Usage.InputOutput, vbo[0]);
  String pgmSrc = join(loadStrings(dataPath("ParticlesDemoProgram.cl")), "\n");
  CLProgram program = context.createProgram(pgmSrc);
  updateParticleKernel = program.build().createKernel("updateParticle");
  callKernel();
}
 
public void draw() {
  queue.finish();
  gl.glClear(GL.GL_COLOR_BUFFER_BIT);
  gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_SRC_COLOR);
  gl.glMatrixMode(GL2.GL_PROJECTION);
  gl.glLoadIdentity();
  pgl.glu.gluOrtho2D(-width/2 - 1, width/2 + 1, -height/2 - 1, height/2 + 1);
  gl.glMatrixMode(GL2.GL_MODELVIEW);
 
  gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vbo[0]);
  gl.glInterleavedArrays(GL2.GL_C4UB_V2F, elementSize, 0);
  gl.glDrawArrays(GL.GL_POINTS, 0, particlesCount);
 
  gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
  callKernel();
}
 
void callKernel() {
  CLEvent kernelCompletion;
  synchronized(updateParticleKernel) {
    interleavedColorAndPositionsMem.acquireGLObject(queue);
    updateParticleKernel.setArgs(massesMem, 
    velocitiesMem, 
    interleavedColorAndPositionsMem.as(Float.class), 
    new float[] {
      mouseX-width/2, height/2-mouseY
    }
    , 
    new float[] {
      width, height
    }
    , 
    2.0, 
    2.0, 
    0.9, 
    0.8, 
    (byte) 0);
 
    int [] globalSizes = new int[] {
      particlesCount
    };
    kernelCompletion = updateParticleKernel.enqueueNDRange(queue, globalSizes);
    interleavedColorAndPositionsMem.releaseGLObject(queue);
  }
}

static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "ParticleSystem" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}