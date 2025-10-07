package code;

import java.sql.*;
import java.util.Scanner;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Admin {

    static Scanner scanner = new Scanner(System.in);

    public static boolean adminMenu() {
        int choice;
        do {
            System.out.println("\n1. View Attendance Record");
            System.out.println("2. View Hourly Record");
            System.out.println("3. Manage Leave Requests");
            System.out.println("4. Log out");
            System.out.print("\nEnter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline character

            switch (choice) {
                case 1:
                    viewAttendanceRecordAdmin();
                    break;
                case 2:
                    viewHourlyRecordAdmin();
                    break;
                case 3:
                    reviewLeaveRequests();
                    break;
                case 4:
                    System.out.println("Logging out...");
                    return false;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (true);

    }

    public static void viewAttendanceRecordAdmin() {
        System.out.print("Enter Employee ID: ");
        int employeeId = scanner.nextInt();
        scanner.nextLine(); // Consume newline character
        System.out.print("Enter Month (MM): ");
        String inputMonth = scanner.nextLine();

        try {
            if (!inputMonth.matches("^(0[1-9]|1[0-2])$")) {
                System.out.println("Invalid month format. Please enter the month in MM format (e.g., 01 for January).");
                return;
            }

            int month = Integer.parseInt(inputMonth);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            // Query attendance, leave records, and holidays for the employee
            String query = "SELECT `Date`, `Time_In`, `Time_Out`, " +
                    "CASE " +
                    "WHEN `Employee_ID` IS NULL AND `Time_In` IS NULL AND `Time_Out` IS NULL THEN 'Holiday' " +
                    "ELSE NULL END AS `status` " +
                    "FROM `attendease_schema`.`attendance record` " +
                    "WHERE (Employee_ID = " + employeeId + " OR Employee_ID IS NULL) " +
                    "AND MONTH(Date) = " + month + " " +
                    "AND YEAR(Date) = " + currentYear + " " +
                    "ORDER BY `Date`";

            ResultSet resultSet = AttendanceSystem.statement.executeQuery(query);

            if (resultSet.next()) {
                System.out.println("|    Date    | Attendance | Time-in | Time-out |");
                System.out.println("+------------+------------+---------+----------+");

                do {
                    String date = resultSet.getString("Date");
                    String timeIn = resultSet.getString("Time_In");
                    String timeOut = resultSet.getString("Time_Out");
                    String leaveStatus = resultSet.getString("status");

                    String attendance;
                    if ("Holiday".equals(leaveStatus)) {
                        // Mark as Holiday
                        attendance = "Holiday";
                        timeIn = "--:--";
                        timeOut = "--:--";
                    } else if (timeIn != null) {
                        // Mark attendance based on time-in and time-out
                        attendance = "Present";
                        timeIn = timeIn.substring(0, 5);  // Format to HH:MM
                        timeOut = (timeOut != null) ? timeOut.substring(0, 5) : "--:--";
                    } else {
                        // Mark as Absent if no time-in, time-out, or holiday
                        attendance = "Absent";
                        timeIn = "--:--";
                        timeOut = "--:--";
                    }

                    System.out.printf("| %s |   %s  |  %s  |   %s  |\n", date, attendance, timeIn, timeOut);
                } while (resultSet.next());
            } else {
                System.out.println("No attendance record found for the specified month.");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public static void viewHourlyRecordAdmin() {
        System.out.print("Enter Employee ID: ");
        int employeeId = scanner.nextInt();
        scanner.nextLine(); // Consume newline character
        System.out.print("Enter Month (MM): ");
        String inputMonth = scanner.nextLine();

        try {
            // Validate Month format (only allow two-digit months)
            if (!inputMonth.matches("^(0[1-9]|1[0-2])$")) {
                System.out.println("Invalid month format. Please enter the month in MM format (e.g., 01 for January).");
                return;
            }

            int month = Integer.parseInt(inputMonth);
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            // Construct the query to retrieve records for the specified month
            String query = "SELECT * FROM `attendease_schema`.`attendance record` WHERE Employee_ID = " + employeeId +
                    " AND MONTH(Date) = " + month + " AND YEAR(Date) = " + currentYear +
                    " ORDER BY Date";
            ResultSet resultSet = AttendanceSystem.statement.executeQuery(query);
            if (resultSet.next()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateFormat.parse(resultSet.getString("Date")));
                int currentMonth = calendar.get(Calendar.MONTH) + 1; // Month is 0-indexed
                int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);

                int week1Start = 1;
                int week2Start = 8;
                int week3Start = 15;
                int week4Start = 22;

                int week1Hours = 0;
                int week2Hours = 0;
                int week3Hours = 0;
                int week4Hours = 0;
                int firstHalfHours = 0;
                int secondHalfHours = 0;
                int totalHours = 0;
                int weekCount = 1;
                int halfMonthCount = 1;

                do {
                    String recordDate = resultSet.getString("Date");
                    String timeIn = resultSet.getString("Time_In").substring(0, 5); // Get only HH:MM
                    String timeOut = resultSet.getString("Time_Out").substring(0, 5); // Get only HH:MM
                    int hoursWorked = AttendanceSystem.calculateHoursWorked(timeIn, timeOut);

                    // Calculate weekly hours
                    calendar.setTime(dateFormat.parse(recordDate));
                    int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                    if (dayOfMonth >= week1Start && dayOfMonth < week2Start) {
                        week1Hours += hoursWorked;
                    } else if (dayOfMonth >= week2Start && dayOfMonth < week3Start) {
                        week2Hours += hoursWorked;
                    } else if (dayOfMonth >= week3Start && dayOfMonth < week4Start) {
                        week3Hours += hoursWorked;
                    } else if (dayOfMonth >= week4Start) {
                        week4Hours += hoursWorked;
                    }

                    // Calculate monthly hours
                    if (dayOfMonth <= 15) {
                        firstHalfHours += hoursWorked;
                    } else {
                        secondHalfHours += hoursWorked;
                    }

                    // Calculate total hours
                    totalHours += hoursWorked;

                    // Increment week count
                    calendar.setTime(dateFormat.parse(recordDate));
                    if (calendar.get(Calendar.WEEK_OF_YEAR) != currentWeek) {
                        currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
                        weekCount++;
                    }

                    // Increment half-month count
                    if (calendar.get(Calendar.MONTH) != currentMonth) {
                        currentMonth = calendar.get(Calendar.MONTH);
                        halfMonthCount++;
                    }
                } while (resultSet.next());

                System.out.println("Date(month): " + AttendanceSystem.getMonthName(month) + " " + currentYear);
                System.out.println("|       Range       | Attended Hour/s | Unattended Hour/s  |");
                System.out.println("|-------------------|-----------------|--------------------|");
                System.out.printf("|       Week 1      |      %3d/48     |         %3d        |\n",
                        Math.min(week1Hours, 48), Math.max(0, (48 - week1Hours))); // Use printf for alignment
                System.out.printf("|       Week 2      |      %3d/48     |         %3d        |\n",
                        Math.min(week2Hours, 48), Math.max(0, (48 - week2Hours)));
                System.out.printf("|       Week 3      |      %3d/48     |         %3d        |\n",
                        Math.min(week3Hours, 48), Math.max(0, (48 - week3Hours)));
                System.out.printf("|       Week 4      |      %3d/48     |         %3d        |\n",
                        Math.min(week4Hours, 48), Math.max(0, (48 - week4Hours)));
                System.out.printf("| 1st half of month |      %3d/96     |         %3d        |\n",
                        Math.min(firstHalfHours, 96), Math.max(0, (96 - firstHalfHours)));
                System.out.printf("| 2nd half of month |      %3d/96     |         %3d        |\n",
                        Math.min(secondHalfHours, 96), Math.max(0, (96 - secondHalfHours)));
                System.out.printf("|       A Month     |      %3d/192    |         %3d        |\n",
                        Math.min(totalHours, 192), Math.max(0, (192 - totalHours)));
            } else {
                System.out.println("No attendance record found for the specified month.");
            }
            // Close resultSet after processing
            try {
                resultSet.close();
            } catch (SQLException e) {
                System.out.println("Error closing ResultSet: " + e.getMessage());
            }
        } catch (SQLException | ParseException e) {
            System.out.println("Database error: " + e.getMessage());
            if (e instanceof ParseException) {
                System.out.println("Invalid month format. Please enter the month in MM format (e.g., 01 for January).");
            }
        }
    }

    // Method to review leave requests
    private static void reviewLeaveRequests() {
        System.out.println("Pending Leave Requests:");
        try {
            String query = "SELECT * FROM `attendease_schema`.`leave_requests` WHERE status = 'Pending'";
            ResultSet resultSet = AttendanceSystem.statement.executeQuery(query);

            // Check if there are any pending requests
            if (!resultSet.next()) {
                System.out.println("No Pending Request");
            } else {
                // If there are pending requests, process and display each one
                do {
                    int leaveId = resultSet.getInt("leave_id");
                    int employeeId = resultSet.getInt("employee_id");
                    String leaveDate = resultSet.getString("leave_date");
                    String reason = resultSet.getString("reason");

                    System.out.println("Leave ID: " + leaveId);
                    System.out.println("Employee ID: " + employeeId);
                    System.out.println("Date: " + leaveDate);
                    System.out.println("Reason: " + reason);
                    System.out.print("Approve or Decline? (A/D): ");
                    String decision = scanner.nextLine().toUpperCase();

                    String updateStatus = decision.equals("A") ? "Accepted" : "Declined";
                    String updateQuery = "UPDATE `leave_requests` SET status = ? WHERE leave_id = ?";
                    PreparedStatement preparedStatement = AttendanceSystem.connection.prepareStatement(updateQuery);
                    preparedStatement.setString(1, updateStatus);
                    preparedStatement.setInt(2, leaveId);
                    preparedStatement.executeUpdate();
                    System.out.println("Leave request " + (updateStatus.equals("Accepted") ? "approved" : "declined") + ".");
                } while (resultSet.next());
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}