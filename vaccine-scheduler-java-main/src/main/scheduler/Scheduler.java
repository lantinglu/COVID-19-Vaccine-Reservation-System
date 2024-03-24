package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.*;
import scheduler.util.Util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!checkStrongPassword(password)) {
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }


    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!checkStrongPassword(password)) {
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean checkStrongPassword(String password) {
        String regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@,#,!,?]).{8,}$";
        if (!password.matches(regex)) {
            System.out.println("This password is not strong.");
            return false;
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
            e.printStackTrace();
        }
        // check if the login process was successful
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }
        try {
            Date date = Date.valueOf(tokens[1]);
            List<Availabilities> availabilities = new Availabilities().getAvailabilities(date);
            if (availabilities.size() == 0) {
                System.err.println("No Caregiver is available on this day");
            } else {
                List<String> usernames = new ArrayList<>();
                for (Availabilities availability : availabilities) {
                    usernames.add(availability.getUsername());
                }
                Collections.sort(usernames);
                for (String username : usernames) {
                    System.out.println(username);
                }
                List<Vaccine> allVaccines = new Vaccine().getAllVaccines();
                if (allVaccines.size() == 0) {
                    System.err.println("No vaccine is available available!");
                } else {
                    for (Vaccine allVaccine : allVaccines) {
                        System.out.println(allVaccine.getVaccineName()+" "+allVaccine.getAvailableDoses());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again.");
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again.");
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient");
            return;
        }
        if (currentPatient == null) {
            System.err.println("Please login first");
            return;
        }
        if (tokens.length != 3) {
            System.err.println("Please try again");
            return;
        }
        try{
            Date date = Date.valueOf(tokens[1]);
            String vaccine = tokens[2];
            List<Appointments> patientAppointments = new Appointments().getAppointments("Patient", currentPatient.getUsername());
            List<Availabilities> availabilities = new Availabilities().getAvailabilities(date);
            if (availabilities.isEmpty()) {
                System.out.println("No caregiver is available");
                return;
            }
            Vaccine vaccine1 = new Vaccine.VaccineGetter(vaccine).get();
            if (vaccine1 == null || vaccine1.getAvailableDoses() <= 0) {
                System.out.println("Not enough available doses");
                return;
            }
            Comparator<Availabilities> usernameComparator = new Comparator<Availabilities>() {
                public int compare(Availabilities a1, Availabilities a2) {
                    return a1.getUsername().compareTo(a2.getUsername());
                }
            };
            availabilities.sort(usernameComparator);
            Availabilities firstAvailableCaregiver = availabilities.get(0);
            String caregiverUsername = firstAvailableCaregiver.getUsername();
            //remove does
            vaccine1.decreaseAvailableDoses(1);
            //remove caregiver
            new Availabilities().removeCaregiver(caregiverUsername, date);
            int appointmentId = new Appointments().addAppointmentID(currentPatient.getUsername(), caregiverUsername, date, vaccine);
            System.out.println("Appointment ID " + appointmentId + ", Caregiver username " + caregiverUsername);
        } catch (IllegalArgumentException | SQLException e) {
            System.err.println("Please try again");
        } catch (Exception e) {
            System.err.println("Please try again");
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) throws SQLException {
        // TODO: Extra credit
        if (currentCaregiver == null && currentPatient == null) {
            System.err.println("Please login first");
            return;
        }
        if (tokens.length != 2) {
            System.err.println("Please try again");
            return;
        }
        try{
            Appointments app = new Appointments();
            int id = Integer.parseInt(tokens[1]);
            String type = "";
            String username = "";
            if (currentPatient != null) {
                type = "Patient";
                username = currentPatient.getUsername();
            } else {
                type = "Caregiver";
                username = currentCaregiver.getUsername();
            }
            List<Appointments> appointments = app.showAppointments(type, username);
            List<Integer> ids = new ArrayList<Integer>();
            for (Appointments appointment : appointments) {
                ids.add(appointment.getID());
            }
            if (!ids.contains(id)) {
                System.out.println("You don't have an appointment with ID " + id);
                return;
            } else {
                Appointments appointments1 = specApp(id);
                app.deleteAppointment(id);
                new Availabilities().addAvailability(appointments1.getCareGiver(), appointments1.getDate());
                Vaccine vaccines = new Vaccine.VaccineGetter(appointments1.getVaccine()).get();
                vaccines.increaseAvailableDoses(1);
                System.out.println("Appointment successfully canceled");
            }
        }catch (SQLException e) {
            System.err.println("Please try again");
        } catch (NumberFormatException e) {
            System.err.println("Please try again");
        }
    }

    // Helper method to get a specific appointment with the id
    private static Appointments specApp(int id) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String select = "SELECT * FROM Appointments WHERE id = ?";
        try {
            PreparedStatement selectStatement = con.prepareStatement(select);
            selectStatement.setInt(1, id);
            ResultSet resultSet = selectStatement.executeQuery();

            Appointments app = null;

            while (resultSet.next()) {
                String caregiver = resultSet.getString("CaregiverName");
                String patient = resultSet.getString("PatientName");
                String vaccine = resultSet.getString("Vaccine");
                Date date = resultSet.getDate("Date");
                app = new Appointments(id, patient, caregiver, date, vaccine);
            }
            return app;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Please try again");
        } finally {
            cm.closeConnection();
        }
    }
    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }
        try {
            List<Appointments> appointments = null;
            if (currentPatient != null) {
                appointments = new Appointments().getAppointments("Patient", currentPatient.getUsername());
                Collections.sort(appointments, Comparator.comparingInt(Appointments::getID));
                for (Appointments appointment : appointments) {
                    System.out.println(appointment.getID() + " " + appointment.getVaccine() + " " + appointment.getDate() + " " + appointment.getCareGiver());
                }
            } else if (currentCaregiver != null) {
                appointments = new Appointments().getAppointments("Caregiver", currentCaregiver.getUsername());
                Collections.sort(appointments, Comparator.comparingInt(Appointments::getID));
                for (Appointments appointment : appointments) {
                    System.out.println(appointment.getID() + " " + appointment.getVaccine() + " " + appointment.getDate() + " " + appointment.getPatient());
                }
            }
            if (appointments.isEmpty()) {
                System.out.println("You don't have appointments.");
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        try {
            if (currentCaregiver != null) {
                currentCaregiver = null;
            } else {
                currentPatient = null;
            }
            System.out.println("Successfully logged out");
        } catch (Exception e) {
            System.out.println("Please try again");
        }
    }
}
