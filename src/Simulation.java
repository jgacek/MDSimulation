import java.util.ArrayList;

public class Simulation {
    ArrayList<Atom> atoms;
    double dt = 0.01;
    double dtSquared = this.dt * this.dt;

    // Values used for the Lennard Jones Potential
    // private double eps = 0.977;
    // Assume to be using Argon atoms for now. Can always change later
    // private double sig = 3.40;

    private double distance(double x1, double y1, double x2, double y2){
        return Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1-y2));
    }

    public Simulation() {
        this.atoms = new ArrayList<>();
    }

    public Simulation(ArrayList<Atom> atoms){
        this.atoms = atoms;
    }

    /**
     * The bread and butter of the algorithm. Calculates the force between 2 atoms. At a distance farther than 1.222,
     * the force is negligible.
     * @param a
     *  The first atom to consider
     * @param b
     *  The second atom to consider
     * @return
     *  Returns the potential energy between the 2 atoms. This could be used to make sure that energy is preserved in
     *  the system. More to add onto that later.
     */
    private double lennardJones(Atom a, Atom b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();

        double rSquared = dx*dx + dy*dy;

        System.out.println("Rsquare is " + rSquared);

        // if rSquared is greater than (1.122 * sig)^2 then force is essentially 0
        if (rSquared > 18) {
            return 0.0;
        }

        double attract = 1 / (rSquared * rSquared * rSquared); // (sig^6)/(r^6)
        double repel = attract * attract; // (sig^12)/(r^12)
        double potentialE = 4 * (attract - repel); // 4 * eps * (attract - repel)

        double fOverR = 24 * ((2*repel) - attract) / rSquared;

        double fx = fOverR * dx;
        double fy = fOverR * dy;

        System.out.println("fx is " + fx);

        // a = F/m assume mass is 1 in below
        a.setxAcceleration(fx);
        a.setyAcceleration(fy);

        b.setxAcceleration(-fx);
        b.setyAcceleration(-fy);

        return potentialE;
    }

    /**
     * For all atoms:
     *  1. Update velocities by half a time step
     *  2. Update position given new velocity
     *  3. Update accelerations by force given by Lennard-Jones Potential
     *  4. Update velocities again given new accelerations.
     * Following code does one step of the velocity verlet integration
     */
    public void verlet() {
        // Step 1 and 2 combined for computational efficiency
        for (Atom atom:this.atoms) {

            double newX = atom.getX() + atom.getxVelocity() * dt + 0.5 * atom.getxAcceleration() * dtSquared;
            double newY = atom.getY() + atom.getyVelocity() * dt + 0.5 * atom.getyAcceleration() * dtSquared;
            atom.setX(newX);
            atom.setY(newY);
            // Step 1
            double newVX = atom.getxVelocity() + 0.5 * atom.getxAcceleration() * dt;
            double newVY = atom.getyVelocity() + 0.5 * atom.getyAcceleration() * dt;
            atom.setxVelocity(newVX);
            atom.setyVelocity(newVY);

        }

        // Step 3 (computationally slow). Solve for force and update accelerations of atoms
        double potentialE = 0.0;
        for (int i = 0; i < this.atoms.size(); i++) {
            Atom a = this.atoms.get(i);
            for (int j = 0; j < i; j++) {
                Atom b = this.atoms.get(j);
                potentialE += this.lennardJones(a, b);
            }
        }

        System.out.println("Potential Energy is " + potentialE);

        // Step 4
        for (Atom atom:this.atoms) {
            double newVX = atom.getxVelocity() + 0.5 * atom.getxAcceleration() * dt;
            double newVY = atom.getyVelocity() + 0.5 * atom.getyAcceleration() * dt;
            atom.setxVelocity(newVX);
            atom.setyVelocity(newVY);
        }
    }
}
