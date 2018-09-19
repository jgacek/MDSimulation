import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.ArrayList;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;


public class View {
    static final float TWOTIMESPI = (float) (Math.PI * 2);

    Simulation sim;

    private long window;

    private int width;
    private int height;

    /**
     * Helper function to draw circle. Maps the real position of (x,y) to the corresponding openGL coordinates.
     * @param x
     *  The real x coordinate
     * @param y
     *  The real y coordinate
     * @return
     *  An array of size 2 containing the openGL coordinates of (x,y)
     */
    private float[] mapRealToGL(float x, float y){
        // @todo check why y = 0.0f maps to the bottom of the window instead of the top
        float newX = (x/4) - 1.0f;
        float newY = (y/4) - 1.0f;
//        float newX = (x / (this.width/2)) - 1.0f;
//        float newY = (y / (this.height/2)) - 1.0f;
        float glCoordinates[] = {newX,newY};
        return glCoordinates;
    }

    /**
     * When working in the openGL coordinate system and float numbers, the coordinate (x,y) = (0.0f,0.0f)
     * represents the very center of the screen as opposed to the top left corner as what intuition would say.
     * Therefore we'll have to call a helper function to map the atom positions to the openGL coordinates.
     * @param x
     *  The x coordinate of where the circle is to be placed
     * @param y
     *  The y coordinate of where the circle is to be placed
     * @param r
     *  The radius of the circle
     */
    private void drawCircle(float x, float y, float r){
        glColor3f(1.0f,0.0f,1.0f);
        float glCoordinates[] = mapRealToGL(x,y);
        // reassign x and y to their openGL coordinate equivalents
        x = glCoordinates[0];
        y = glCoordinates[1];
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(x,y);
        for(int i=0; i <= 20; i++) {
            float x2 = (float) (x + r * Math.cos(i * TWOTIMESPI / 20));
            float y2 = (float) (y + r * Math.sin(i * TWOTIMESPI / 20));
            glVertex2f(x2,y2);
        }
        glEnd();
    }

    /**
     * Draws all the atoms on screen 1 by 1. Makes a call to draw circle for each atom
     */
    private void drawAtoms(){
        for (Atom atom:this.sim.atoms) {
            // Probably not very efficient to change all to floats. Should be more consistent throughout program.
            drawCircle((float) atom.getX(),(float) atom.getY(),(float) atom.getR());
        }
    }

    private void init(){
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        this.window = glfwCreateWindow(this.width, this.height, "MDSimulation", NULL, NULL);
        if ( this.window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(this.window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(this.window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    this.window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            drawAtoms();

            this.sim.verlet();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    public View(Simulation sim){
        this.sim = sim;
        this.width = 800;
        this.height = 800;
    }

    public static void main(String args[]) {
        Atom a = new Atom(0.14,0,0,0,0, 3,4);
        Atom b = new Atom(0.14,0,0,0,0,4,4);

        ArrayList<Atom> atoms = new ArrayList<>();
        atoms.add(a);
        atoms.add(b);

        Simulation sim = new Simulation(atoms);
        View view = new View(sim);
        view.init();
        view.loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(view.window);
        glfwDestroyWindow(view.window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

}
