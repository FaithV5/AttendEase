package code;

import java.sql.*;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

class AttendanceSystem {

    static Connection connection;
    static Statement statement;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Database Connection
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/attendease_schema", "root", "november5");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error connecting to database: " + e.getMessage());
            System.exit(1);
        }

        while (true) {  // Loop back to the login page after logout
            boolean loggedIn = false;
            int loginAttempts = 0;

            // Login Page
            while (loginAttempts < 5 && !loggedIn) {
                System.out.println("\nHEMAMIVA Attendance Monitoring");
                System.out.println("---------Log In Page--------");
                System.out.print("\nEnter ID: ");
                int employeeId = scanner.nextInt();
                scanner.nextLine(); // Consume newline character
                System.out.print("Password: ");
                String password = scanner.nextLine();

                try {
                    // Admin Login Query
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM `attendease_schema`.`admin` WHERE `Admin_ID` = " + employeeId);
                    if (resultSet.next()) {
                        String storedPassword = resultSet.getString("Password");
                        if (storedPassword.equals(password)) {
                            loggedIn = true;
                            String firstName = resultSet.getString("First_Name");
                            String lastName = resultSet.getString("Last_Name");
                            System.out.println("\nWelcome Admin " + firstName + " " + lastName);
                            if (!Admin.adminMenu()) {
                                break;  // Logout returns to login
                            }
                        }
                    } else {
                        // Employee Login Query
                        resultSet = statement.executeQuery("SELECT * FROM `attendease_schema`.`employee` WHERE `Employee_ID` = " + employeeId);
                        if (resultSet.next()) {
                            String storedPassword = resultSet.getString("Password");
                            if (storedPassword.equals(password)) {
                                loggedIn = true;
                                String firstName = resultSet.getString("First_Name");
                                String lastName = resultSet.getString("Last_Name");
                                System.out.println("\nWelcome " + firstName + " " + lastName);
                                mainMenu(firstName, lastName, employeeId);
                                break;  // Logout returns to login
                            } else {
                                System.out.println("Incorrect password. Please try again.");
                                loginAttempts++;
                            }
                        } else {
                            System.out.println("Invalid ID. Please try again.");
                            loginAttempts++;
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }
            }

            if (loginAttempts >= 5) {
                System.out.println("Too many login attempts. System closing.");
                System.exit(0);
            }
        }
    }

    // Main Menu
    private static void mainMenu(String firstName, String lastName, int employeeId) {
        int choice;
        do {
            System.out.println("\n1. Record Attendance");
            System.out.println("2. View Attendance Record");
            System.out.println("3. View Hourly Record");
            System.out.println("4. Report Leave");
            System.out.println("5. View Leave Record");
            System.out.println("6. Log out");
            System.out.print("\nEnter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline character

            switch (choice) {
                case 1:
                    recordAttendance(employeeId);
                    break;
                case 2:
                    viewAttendanceRecord(employeeId);
                    break;
                case 3:
                    viewHourlyRecord(employeeId);
                    break;
                case 4:
                    reportLeave(employeeId);
                    break;
                case 5:
                    viewLeaveRecord(employeeId);
                    break;
                case 6:
                    System.out.println("Logging out...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 6);
    }

    // Record Attendance
    private static void recordAttendance(int employeeId) {
        System.out.println("Date: " + getCurrentDate());

        try {
            // Check if attendance record already exists for the day
            ResultSet resultSet = statement.executeQuery("SELECT * FROM `attendease_schema`.`attendance record` WHERE `Employee_ID` = " + employeeId + " AND Date = '" + getCurrentDate() + "'");

            String timeIn = null;
            String timeOut = null;

            // Check if record exists for today and retrieve Time In and Time Out if available
            if (resultSet.next()) {
                timeIn = resultSet.getString("Time_In");
                timeOut = resultSet.getString("Time_Out");

                if (timeIn != null && timeOut == null) {
                    // Show the existing Time In and ask for Time Out if Time Out is missing
                    System.out.println("Existing Time In: " + timeIn);
                    System.out.print("Enter Time Out (HH:MM): ");
                    timeOut = scanner.nextLine();

                    // Validate Time Out format
                    if (!isValidTimeFormat(timeOut)) {
                        System.out.println("Invalid time format. Please enter time in HH:MM format.");
                        return;
                    }

                    // Get current time in HH:MM format
                    String currentTime = getCurrentTime();

                    // Check if the entered time-out exactly matches the current time
                    if (!timeOut.equals(currentTime)) {
                        System.out.println("Entered time-out does not match the current time. Please enter the exact current time.");
                        return;
                    }

                    // Update the record with Time Out
                    String updateQuery = "UPDATE `attendease_schema`.`attendance record` SET `Time_Out` = '" + timeOut + "' WHERE `Employee_ID` = " + employeeId + " AND Date = '" + getCurrentDate() + "'";
                    statement.executeUpdate(updateQuery);
                    System.out.println("Time Out recorded successfully.");
                } else {
                    System.out.println("Attendance record already exists for today with Time In and Time Out.");
                }
            } else {
                // If no record exists for today, ask for Time In
                System.out.print("Enter Time In (HH:MM): ");
                timeIn = scanner.nextLine();

                // Validate Time In format
                if (!isValidTimeFormat(timeIn)) {
                    System.out.println("Invalid time format. Please enter time in HH:MM format.");
                    return;
                }

                // Get current time in HH:MM format
                String currentTime = getCurrentTime();

                // Check if the entered time-in exactly matches the current time
                if (!timeIn.equals(currentTime)) {
                    System.out.println("Entered time-in does not match the current time. Please enter the exact current time.");
                    return;
                }

                // Insert a new attendance record with Time In
                String insertQuery = "INSERT INTO `attendease_schema`.`attendance record` (`Employee_ID`, `Date`, `Time_In`, `Time_Out`) VALUES (" + employeeId + ", '" + getCurrentDate() + "', '" + timeIn + "', NULL)";
                statement.executeUpdate(insertQuery);
                System.out.println("Time In recorded successfully.");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // View Attendance Record
    private static void viewAttendanceRecord(int employeeId) {
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

            ResultSet resultSet = statement.executeQuery(query);

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

    // View Hourly Record
    private static void viewHourlyRecord(int employeeId) {
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
            ResultSet resultSet = statement.executeQuery(query);
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
                    int hoursWorked = calculateHoursWorked(timeIn, timeOut);

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

                System.out.println("Date(month): " + getMonthName(month) + " " + currentYear);
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

    // Method to report a leave
    private static void reportLeave(int employeeId) {
        System.out.print("Enter the date you wish to apply for leave (YYYY-MM-DD): ");
        String leaveDate = scanner.nextLine();
        System.out.print("Enter reason for leave: ");
        String reason = scanner.nextLine();

        try {
            String query = "INSERT INTO `attendease_schema`.`leave_requests` (employee_id, leave_date, reason, status) VALUES (?, ?, ?, 'Pending')";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, employeeId);
            preparedStatement.setString(2, leaveDate);
            preparedStatement.setString(3, reason);
            preparedStatement.executeUpdate();
            System.out.println("Leave application submitted. Status: Pending");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // View Leave Record
    private static void viewLeaveRecord(int employeeId) {
        System.out.println("\n--- Leave Record ---");
        try {
            String query = "SELECT * FROM `attendease_schema`.`leave_requests` WHERE `employee_id` = " + employeeId + " ORDER BY `leave_date` DESC";
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                System.out.println("| Leave Date | Reason |  Status  |");
                System.out.println("+------------+--------+----------+");
                do {
                    String leaveDate = resultSet.getString("leave_date");
                    String reason = resultSet.getString("reason");
                    String status = resultSet.getString("status");
                    System.out.printf("| %s |  %s  | %s |\n", leaveDate, reason, status);
                } while (resultSet.next());
            } else {
                System.out.println("No leave records found.");
            }
            resultSet.close();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public static int calculateHoursWorked(String timeIn, String timeOut) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        Date date1 = format.parse(timeIn);
        Date date2 = format.parse(timeOut);
        long difference = date2.getTime() - date1.getTime();
        return (int) (difference / (1000 * 60 * 60));
    }

    private static boolean isValidTimeFormat(String time) {
        return time.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]");
    }

    public static String getAttendance(String date, String timeIn) {
        return (timeIn.compareTo("08:00") <= 0 ? "Present" : "Late");
    }

    private static String getCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(new Date());
    }

    public static String getMonthName(int month) {
        return new DateFormatSymbols().getMonths()[month - 1];
    }

    // Helper method to get the current time in HH:MM format
    private static String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        return formatter.format(new Date());
    }

    // Helper method to check if time-in is close to the current time (within a +/-10-minute range)
    private static boolean isTimeCloseToCurrent(String enteredTime, String currentTime) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            Date entered = timeFormat.parse(enteredTime);
            Date current = timeFormat.parse(currentTime);

            // Calculate the difference in milliseconds
            long difference = Math.abs(current.getTime() - entered.getTime());

            // Allow a 10-minute range (10 minutes * 60 seconds * 1000 milliseconds)
            long allowedRange = 10 * 60 * 1000;

            return difference <= allowedRange;
        } catch (ParseException e) {
            System.out.println("Error parsing time: " + e.getMessage());
            return false;
        }
    }
}