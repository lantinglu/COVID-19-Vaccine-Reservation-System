package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;


public class Availabilities {
    private Date Time;
    private String Username;

    public Availabilities(Date time, String username) {
        Time = time;
        Username = username;
    }
    public Availabilities() {}

    public Date getTime() {return Time;}

    public String getUsername() {return Username;}

    public List<Availabilities> getAvailabilities(Date date)  throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        List<Availabilities> availabilities = new ArrayList<Availabilities>();
        String getAvailabilites = "SELECT * FROM Availabilities WHERE Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(getAvailabilites);
            statement.setDate(1, date);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String username = resultSet.getString("Username");
                Date time = resultSet.getDate("Time");
                availabilities.add(new Availabilities(time, username));
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
        return availabilities;
    }

    public void removeCaregiver(String name, Date date) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String removeCaregiver = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(removeCaregiver);
            statement.setString(1, name);
            statement.setDate(2, date);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void addAvailability(String username, Date date) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setDate(1, date);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error occurred when uploading Availability!");
        } finally {
            cm.closeConnection();
        }
    }
}
