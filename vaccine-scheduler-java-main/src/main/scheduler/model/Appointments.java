package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.*;
import java.util.*;
import java.sql.Date;


public class Appointments {
    private int id;
    private String patient;
    private String careGiver;
    private Date date;
    private String vaccine;

    public Appointments(int id, String patient, String careGiver, Date date, String vaccine) {
        this.id = id;
        this.patient = patient;
        this.careGiver = careGiver;
        this.date = date;
        this.vaccine = vaccine;
    }

    public Appointments() {}

    public String getPatient() {return patient;}
    public String getCareGiver() {return careGiver;}
    public Date getDate() {return date;}
    public String getVaccine() {return vaccine;}
    public int getID() {return id;}

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String addAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setDate(1, date);
            statement.setString(2, vaccine);
            statement.setString(3, patient);
            statement.setString(4, careGiver);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when adding new appointment!");
        } finally {
            cm.closeConnection();
        }
    }

    public List<Appointments> getAppointments(String type, String username) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        List<Appointments> appointments = new ArrayList<>();
        String getAppointment = "";
        if (type.equals("Patient")) {
            getAppointment = "SELECT * FROM Appointments WHERE PatientName = ?";
        } else if(type.equals("Caregiver")){
            getAppointment = "SELECT * FROM Appointments WHERE CaregiverName = ?";
        }
        try {
            PreparedStatement statement = con.prepareStatement(getAppointment);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String caregiver = resultSet.getString("CaregiverName");
                String patient = resultSet.getString("PatientName");
                String vaccine = resultSet.getString("Vaccine");
                java.sql.Date date = resultSet.getDate("Date");
                appointments.add(new Appointments(id, patient, caregiver, date, vaccine));
            }
            return appointments;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public int addAppointmentID(String patient, String caregiver, Date date, String vaccine) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String addAppointment = "INSERT INTO Appointments (PatientName, CaregiverName, Date, Vaccine) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, patient);
            statement.setString(2, caregiver);
            statement.setDate(3, date);
            statement.setString(4, vaccine);
            statement.executeUpdate();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException();
            }
        } finally {
            cm.closeConnection();
        }
    }

    public List<Appointments> showAppointments(String type, String username) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        List<Appointments> appointments = new ArrayList<>();

        String findAppointment;
        if (type.equals("Patient")) {
            findAppointment = "SELECT * FROM Appointments WHERE PatientName = ?";
        } else {
            findAppointment = "SELECT * FROM Appointments WHERE CaregiverName = ?";
        }
        try {
            PreparedStatement statement = con.prepareStatement(findAppointment);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String caregiver = resultSet.getString("CaregiverName");
                String patient = resultSet.getString("PatientName");
                String vaccine = resultSet.getString("Vaccine");
                java.sql.Date date = resultSet.getDate("Date");
                appointments.add(new Appointments(id, patient, caregiver, date, vaccine));
            }
            return appointments;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void deleteAppointment(int id) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String cancel = "DELETE FROM Appointments WHERE id = ?";
        try {
            PreparedStatement cancelStatement = con.prepareStatement(cancel);
            cancelStatement.setInt(1, id);
            cancelStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when canceling appointment!");
        } finally {
            cm.closeConnection();
        }
    }
}
