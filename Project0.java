import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList; // Is that why servers are so big? Is it stored in the RAM?
// Note: writing to a table also adds it to the ArrayList
// Note: Don't trust the str1==str2 operator when using a string from Scanner. Use str1.equals(str2)
// TODO: 7. A minimum of one (1) JUnit test is written to test some functionality.

class Account {
    private String username;
    private String password;
    String getUsername() {
        return username;
    }
    void setUsername(String username) {
        this.username = username;
    }
    String getPassword() {
        return password;
    }
    void setPassword(String password) {
        this.password = password;
    }
}

class CustomerAccount extends Account {
    private double balance = 0; // no max balance
    boolean approved = false;
    
    void deposit(double mon) {
        if(mon>0) balance += mon;
        else {
            System.out.println("Deposit cannot be negative");
        } 
    }
    void withdraw(double mon) {
        if(mon>0) {
            if(mon<=balance) balance -= mon;
            else {
                System.out.println("The amount entered is greater than the account's balance");
            } 
        }
        else System.out.println("Deposit cannot be negative");
    
    }
    double getBalance() {
        return balance;
    }
    
    Transaction sendTransaction(double mon, String receiverStr) {
        // return type is only use in the overloaded version of this method
        ResultSet tempRS = DaoFactory.getResultSet("select * from customer;");
        boolean keepSearching = true;
        try {
            while(tempRS.next() && keepSearching) {
                if(tempRS.getString(1).equals(receiverStr)) {
                    keepSearching = false;
                }
            }
        } catch (SQLException e) {
            System.out.println("[Error in CustomerAccount.sendTransaction()]: ");
            e.printStackTrace();
        }
        if(keepSearching) { // if search finds nothing
            System.out.println("invalid receiver");
            return null;
        }
        if(mon > 0) {
            withdraw(mon);
            System.out.println("Money sent to "+ receiverStr);
            LinkSQL.writeToLogTable(getUsername() +" sent "+ mon +" to "+ receiverStr);
        }
        else if (mon < 0) {
            System.out.println("Request sent to "+ receiverStr);
            LinkSQL.writeToLogTable(getUsername() +" requested "+ mon +" from "+ receiverStr);
        }
        Transaction t = new Transaction();
        t.sender = getUsername();
        t.receiver = receiverStr;
        t.amount = mon;
        sendTransactionB(t);
        return t;
    }
    
    void sendTransaction(double mon, String receiverStr, String note) {
        Transaction t = sendTransaction(mon, receiverStr);
        t.note = note;
        System.out.println("You sent a note to "+ receiverStr);
        LinkSQL.writeToLogTable(getUsername() +" sent a note to "+ receiverStr +": "+ note);
        sendTransactionB(t);
    }
    
    private void sendTransactionB(Transaction t) { // to help me reuse code :)
        LinkSQL.writeToTransactionTable(t);
    }
    
}

class EmployeeAccount extends Account {}

class Transaction {
    String sender, receiver;
    String note;
    double amount;
}

class LinkSQL {
    static ArrayList<CustomerAccount> getCustomerAccounts() { // reads the SQL table, inefficient
        ArrayList<CustomerAccount> tempArrList = new ArrayList<CustomerAccount>();
        ResultSet tempRS =  DaoFactory.getResultSet("select * from customer;");
        
        try {
            while(tempRS.next()) {
                boolean approve;
                if(tempRS.getInt(4)==0) approve = false;
                else approve = true;
                
                CustomerAccount tempAcc = new CustomerAccount();
                tempAcc.setUsername(tempRS.getString(1));
                tempAcc.setPassword(tempRS.getString(2));
                if(tempRS.getDouble(3) > 0) tempAcc.deposit(tempRS.getDouble(3));
                tempAcc.approved = approve;
                
                tempArrList.add(tempAcc);
            }
        } catch (SQLException e) {
            System.out.println("[Error in LinkSQL.getCustomerAccounts()]: ");
            e.printStackTrace();
        }
        DaoFactory.closeResultSet();
        return tempArrList;
    }
    static ArrayList<EmployeeAccount> getEmployeeAccounts() { // reads the SQL table
        ArrayList<EmployeeAccount> tempArrList = new ArrayList<EmployeeAccount>();
        ResultSet tempRS =  DaoFactory.getResultSet("select * from employee;");
        try {
            while(tempRS.next()) {
                EmployeeAccount tempAcc = new EmployeeAccount();
                tempAcc.setUsername(tempRS.getString(1));
                tempAcc.setPassword(tempRS.getString(2));
                
                tempArrList.add(tempAcc);
            }
        } catch (SQLException e) {
            System.out.println("[Error in LinkSQL.getEmployeeAccounts()]: ");
            e.printStackTrace();
        }
        DaoFactory.closeResultSet();
        return tempArrList;
    }
    
    static ArrayList<Transaction> getTransactionList() {
        ArrayList<Transaction> tempArrList = new ArrayList<Transaction>();
        ResultSet tempRS =  DaoFactory.getResultSet("select * from \"transaction\";");
        try {
            while(tempRS.next()) {
                Transaction t = new Transaction();
                t.sender = tempRS.getString(1);
                t.receiver = tempRS.getString(2);
                t. amount = tempRS.getDouble(3);
                // t.note = tempRS.getString(4);
                tempArrList.add(t);
            }
        } catch (SQLException e) {
            System.out.println("[Error in LinkSQL.getTransactionList()]: ");
            e.printStackTrace();
        }
        DaoFactory.closeResultSet();
        return tempArrList;
    }
    
    static void writeToCustomerTable(CustomerAccount acc) { // for new accounts
        int approved = acc.approved ? 1:0;
        DaoFactory.getPreparedStatement("insert into \"customer\" values(\"?\", \"?\", ?, ?);");
        try {        
            DaoFactory.getPreparedStatement().setString(1, acc.getUsername());
            DaoFactory.getPreparedStatement().setString(2, acc.getPassword());
            DaoFactory.getPreparedStatement().setDouble(3, acc.getBalance());
            DaoFactory.getPreparedStatement().setInt(4, approved);
            DaoFactory.getPreparedStatement().executeQuery(); // returns ResultSet
        } catch (SQLException e) {
            System.out.println("[Error in LinkSQL.writeToCustomerTable()]: ");
            e.printStackTrace();
        }        
        DaoFactory.closePreparedStatement();
        LinkSQL.writeToLogTable("New Customer account '"+ acc.getUsername() +"' created and stored.");

        System.out.println("New Customer account '"+ acc.getUsername() +"' created and stored.");
        Project0.customerAccounts.add(acc);
    }
    
    static void writeToEmployeeTable(EmployeeAccount acc) { // for new accounts
        DaoFactory.getPreparedStatement("insert into employee values(\"?\", \"?\");");
        try {
            DaoFactory.getPreparedStatement().setString(1, acc.getUsername());
            DaoFactory.getPreparedStatement().setString(2, acc.getPassword());
            DaoFactory.getPreparedStatement().executeQuery(); // returns ResultSet
        } catch (SQLException e) {
            System.out.println("[Error in LinkSQL.writeToEmployeeTable()]: ");
            e.printStackTrace();
        }        
        DaoFactory.closePreparedStatement();
        System.out.println("New Employee account '"+ acc.getUsername() +"' created and stored.");
        LinkSQL.writeToLogTable("New Employee account '"+ acc.getUsername() +"' created and stored.");
        Project0.employeeAccounts.add(acc);
    }
    
    static void writeToTransactionTable(Transaction t) { // Not a log, think of it like a mailbox
        DaoFactory.getPreparedStatement("insert into \"transaction\" values(\"?\", \"?\", ?);");
        try {
            DaoFactory.getPreparedStatement().setString(1, t.sender);
            DaoFactory.getPreparedStatement().setString(2, t.receiver);
            DaoFactory.getPreparedStatement().setDouble(3, t.amount);
            DaoFactory.getPreparedStatement().executeQuery(); // returns ResultSet
        } catch (SQLException e) {
            System.out.println("[Error in LinkSQL.writeToTransactionTable()]: ");
            e.printStackTrace();
        }        
        DaoFactory.closePreparedStatement();
        System.out.println("A new transaction is made.");
        LinkSQL.writeToLogTable("A new transaction is made.");
        Project0.transactions.add(t);
    }
    
    static void writeToLogTable(String strLog) {
        DaoFactory.getPreparedStatement("insert into log values(\"?\", \"?\");");
        try {
            DaoFactory.getPreparedStatement().setString(1, DateTimeConverter.getString());
            DaoFactory.getPreparedStatement().setString(2, strLog);
            DaoFactory.getPreparedStatement().executeQuery();            
        } catch (SQLException e) {
            System.out.println("[Error in LinkSQL.writeToLogTable()]: ");
            e.printStackTrace();
        }
        // System.out.println("New log '"+ strLog +"' should be created and stored.");
        DaoFactory.closePreparedStatement();
    }
    
    static void printLog() { 
        // works for any size (any number of rows and columns)
        ResultSet rs = DaoFactory.getResultSet("select * from log;");
        printFancyLog();
        try {
            while (rs.next()) {
                for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {
                    System.out.print("\t"+ rs.getObject(i));
                }
                System.out.print("\n");
            }
        } catch (SQLException e) {
            System.out.println("[Error in LinkSQL.printLog()]: ");
            e.printStackTrace();
        }    
    }
    static boolean printFancyLog() { // TODO: testing
        DaoFactory.getResultSet(".mode table");
        ResultSet rs = DaoFactory.getResultSet("select * from log;");
        System.out.println(rs);
        return true;
    }

}
public class Project0 {
    static boolean restart = false; // only one user can login at the same time
    static ArrayList<CustomerAccount> customerAccounts;
    static ArrayList<EmployeeAccount> employeeAccounts;
    static ArrayList<Transaction> transactions;

    static CustomerAccount askCustomerUsernamePassword() { // returns null if login unsuccessful
        String username;
        String password;
        System.out.print("Username: ");
        username = DaoFactory.getScanner().next();
        for (CustomerAccount acc : customerAccounts) { // check 1st column only (SQL)
            if (acc.getUsername().equals(username)) {
                System.out.print("Password (not hidden): ");
                password = DaoFactory.getScanner().next();
                if(acc.getPassword().equals(password)) {
                    if(acc.approved) return acc;
                    else System.out.println("Account not yet approved");
                    // LinkSQL.writeToLogTable("");
                    
                }
                else {
                    System.out.println("invalid password");
                    break;
                }
            }
            // loading...
        }
        System.out.println("Invalid login. Try again.");
        DaoFactory.getScanner().next();
        restart = true;
        return null;
        // System.exit(0);
    }
    // literally a copy of the above, but with different types
    static EmployeeAccount askEmployeeUsernamePassword() { // returns null if login unsuccessful
        String username;
        String password;
        System.out.print("Username: ");
        username = DaoFactory.getScanner().next();
        for (EmployeeAccount acc : employeeAccounts) {
            if (acc.getUsername().equals(username)) {
                System.out.print("Password (not hidden): ");
                password = DaoFactory.getScanner().next();
                if(acc.getPassword().equals(password)) {
                    return acc;
                }
                else {
                    System.out.println("invalid password");
                    break;
                }
            }
            // loading...
        }
        System.out.println("Invalid login. Try again.");
        DaoFactory.getScanner().next();
        restart = true;
        return null;
        // System.exit(0);
    }
    static void customerOptions(CustomerAccount acc) {
        boolean goBack = false;
        String customerWelcome = "What would you like to do? Type a command:"
            + "\n\t" + "balance = View balance"
            + "\n\t" + "deposit = Make deposit" 
            + "\n\t" + "withdraw = Make withdrawal"
            + "\n\t" + "send = Send money to another account"
            + "\n\t" + "exit = exit";

        // Scanner scan = new Scanner(System.in);
        do {
            System.out.println(customerWelcome);
            switch (DaoFactory.getScanner().nextLine()) {
                case "balance":
                    System.out.println("Your balance is "+ acc.getBalance());
                    goBack = true;
                    break;
                case "deposit":
                    System.out.println("Your balance is "+ acc.getBalance());
                    System.out.print("Deposit how much? ");
                    String tempStr1 = DaoFactory.getScanner().next();
                    Double tempDou1 = Double.parseDouble(tempStr1);                    
                    acc.deposit(tempDou1);
                    System.out.println("You deposited "+ tempDou1);
                    String tempLog1 = acc.getUsername()+" deposited "+tempDou1+"into their account.";
                    LinkSQL.writeToLogTable(tempLog1);
                    System.out.println("Your balance is now "+ acc.getBalance());
                    goBack = true;
                    break;
                case "withdraw":
                    System.out.println("Your balance is "+ acc.getBalance());
                    System.out.print("Withdraw how much? ");
                    String tempStr2 = DaoFactory.getScanner().next();
                    Double tempDou2 = Double.parseDouble(tempStr2);  
                    acc.withdraw(tempDou2);
                    System.out.println("You withdrew "+ tempDou2);
                    String tempLog2 = acc.getUsername()+" withdrew "+tempDou2+"from their account.";
                    LinkSQL.writeToLogTable(tempLog2);
                    System.out.println("Your balance is now "+ acc.getBalance());
                    goBack = true;
                    break;
                case "send":
                    String temp1, temp2;
                    System.out.print("Send to who? ");
                    temp1 = DaoFactory.getScanner().next();
                    // CustomerAccount temp4 = LinkSQL.findCustomerAccount(temp1);
                    System.out.print("How much? ");
                    temp2 = DaoFactory.getScanner().next();
                    Double temp3 = Double.parseDouble(temp2);
                    System.out.println("Would you like to attach a note to the transaction? If so, type yes.");
                    switch (DaoFactory.getScanner().nextLine()) {
                        case "yes":
                            System.out.println("Note: ");
                            acc.sendTransaction(temp3, temp1, DaoFactory.getScanner().next());
                            break;
                        default:
                            acc.sendTransaction(temp3, temp1);
                            break;
                    }         
                    break;
                case "exit":
                    goBack = false;
                    break;
                default:System.out.println("Invalid input");
                    goBack = true;
                    break;
            }
        } while (goBack);

        // DaoFactory.closeScanner();
    }
    static void employeeOptions(EmployeeAccount empAcc) { // empAcc is unused
        boolean goBack = false;
        String employeeWelcome = "Type command:"
            + "\n\t" + "accounts = Approve or deny an account"
            + "\n\t" + "balance = View an account's balance"
            + "\n\t" + "log = View log of all transactions and other activities"
            + "\n\t" + "exit = exit";
        // Scanner scan = new Scanner(System.in);
        do {
            goBack = false;
            System.out.println(employeeWelcome);
            switch (DaoFactory.getScanner().nextLine()) {
                case "accounts":
                    // inefficiently check each account if it's unapproved
                    // instead of already having a separate list of account to approve
                    for (CustomerAccount acc : customerAccounts) {
                        if(!acc.approved) {
                            System.out.println("Approve "+ acc.getUsername() +"? (yes/no)");
                            boolean temp = false;
                            do {
                                switch (DaoFactory.getScanner().nextLine()) {
                                    case "yes": acc.approved = true;
                                        break;
                                    case "no": // do nothing
                                        break;
                                    default: System.out.println("Invalid input");
                                        temp = true;
                                        break;
                                }
                            } while (temp);

                        }
                    }
                    goBack = true;
                    break;
                case "balance":
                    // inefficiently check each account's username
                    // instead of already having a separate list of account names
                    System.out.print("Type a username: ");
                    String temp = DaoFactory.getScanner().next();
                    for (CustomerAccount acc : customerAccounts) {
                        if (acc.getUsername().equals(temp)) {
                            System.out.println(acc.getUsername() +"'s balance is "+ acc.getBalance());
                            break;
                        }
                        // loading...
                    }
                    goBack = true;      
                    break;
                case "log":
                    LinkSQL.printLog();
                    break;
                case "exit":
                    goBack = false;
                    break;
                default: 
                    System.out.println("Invalid input");
                    goBack = true;
                    break;
            }

        } while (goBack);

        // DaoFactory.closeScanner();;
    }
    
    static void createNewCustomerAccount() {
        boolean loopBack;
        boolean canceling = false;
        CustomerAccount temp1;
        do {
            temp1 = new CustomerAccount();
            System.out.print("Create a username (enter c to cancel): ");
            String tempUserStr = DaoFactory.getScanner().next();
            if(tempUserStr.equals("c")) {
                System.out.println("canceling...");
                System.out.println("tempUserStr is "+tempUserStr);
                canceling = true;
                loopBack = false; // unnecessary
                return;
            }
            
            ResultSet tempRS = DaoFactory.getResultSet("select * from customer;");
            boolean isSearching = true;
            try {
                while(tempRS.next() && isSearching) {
                    if(tempRS.getString(1).equals(tempUserStr)) {
                        System.out.println("Username is already taken");
                        // stop searching and loop back, use "continue" keyword
                        isSearching = false;
                        break; // break and keepSearch are redundant and serve the same purpose
                    }
                }
            } catch (SQLException e) {
                System.out.println("[Error in createNewCustomerAccount()]: ");
                e.printStackTrace();
            }
            if(isSearching) { // if username is not taken
                temp1.setUsername(tempUserStr);
                loopBack = false; // "break" from the loop
            }
            else { // else when username is taken
                loopBack = true;
            }
        } while (loopBack);
        
        if(!canceling) {
            System.out.print("Create a password (not hidden): ");
            temp1.setPassword(DaoFactory.getScanner().next());
            
            LinkSQL.writeToCustomerTable(temp1);

            String tempSqlStr = String.format("insert into customer values(\"%1$s\",\"%2$s\",0.0,0);",
            temp1.getUsername(),
            temp1.getPassword()
            );
            System.out.println(tempSqlStr);
            DaoFactory.getResultSet(tempSqlStr);
            LinkSQL.writeToLogTable("New account '"+ temp1.getUsername()+"' created "); // print new account username
        }
        // DaoFactory.closeScanner();
    }
    
    static void acceptTransactions(CustomerAccount acc) {
        // deposit(mon);
        ResultSet tempRS = DaoFactory.getResultSet("select * from \"transaction\";");
        try {
            String receiverStr = acc.getUsername();
            while(tempRS.next()) {
                if(tempRS.getString(2).equals(receiverStr)) {
                    String senderStr = tempRS.getString(1);
                    double mon = tempRS.getDouble(3);
                    String note = tempRS.getString(4);
                    if(mon > 0 ) {
                        System.out.println("You received a transaction of "+ mon +" from "+ senderStr);
                        if(note!=null) System.out.println("Note from "+ senderStr +"'s account: " + note);
                        System.out.println("Accept? (Type yes or no. Type anything else to decide later in your next login.)");
                        switch (DaoFactory.getScanner().nextLine()) {
                            case "yes":
                                acc.deposit(mon);
                                acc.sendTransaction(0, senderStr, receiverStr +" accepted your payment.");
                                System.out.println("You accepted the payment of "+ mon);
                                LinkSQL.writeToLogTable(receiverStr +" accepted "+ mon +" from "+ senderStr);
                                break;
                            case "no": // sending back money
                                acc.deposit(mon); // sendTransaction always withdraws money from the account
                                acc.sendTransaction(mon, receiverStr, receiverStr +" declined your transaction." );
                                System.out.println("Transaction declined");
                                LinkSQL.writeToLogTable(receiverStr +" decline a transaction of "+ mon +" from  "+ senderStr);
                                break;
                            default:
                                continue; // do nothing until next login (skip to next loop)
                        }
                    }
                    else if(tempRS.getDouble(3) < 0) {
                        System.out.println(senderStr +" requests "+ mon);
                        if(note!=null) System.out.println("Note from "+ senderStr +"'s account: " + note);
                        System.out.println("Accept? (Type yes or no. Type anything else to decide later in your next login.)");
                        switch (DaoFactory.getScanner().nextLine()) {
                            case "yes":
                                System.out.println("You accepted "+ senderStr +"'s request");
                                acc.sendTransaction(mon, senderStr);
                                System.out.println("You sent "+ mon +" to "+ senderStr);
                                LinkSQL.writeToLogTable(receiverStr +" accepted "+ senderStr +"'s request of "+ mon);
                                break;
                            case "no":
                                acc.sendTransaction(0, senderStr, receiverStr +" declined your request of "+ mon);
                                LinkSQL.writeToLogTable(receiverStr +" decline a request of "+ mon +" from  "+ senderStr);
                                break;
                            default:
                                continue; // do nothing until next login (skip to next loop)
                        }
                    }
                    else { // tempRS.getDouble(3) == 0
                        if(note!=null) System.out.println("Message from"+ senderStr +"'s account: "+ note);
                        else System.out.println(senderStr +" poked you.");
                        DaoFactory.getScanner().nextLine();
                    }
                    
                }
            }
        } catch (SQLException e) {
            System.out.println("[Error in createNewCustomerAccount()]: ");
            e.printStackTrace();
        }
    
    }
    // TODO: debug
    public static void main(String[] args) {
        CustomerAccount temp1 = null;
        EmployeeAccount temp2 = null;
        customerAccounts = LinkSQL.getCustomerAccounts();
        employeeAccounts = LinkSQL.getEmployeeAccounts();
        transactions = LinkSQL.getTransactionList();
        do {
            restart = false;
            System.out.println("Choose a Login:"
            + "\n\t" + "customer = Customer login"
            + "\n\t" + "employee = Employee login"
            + "\n\t" + "apply = Apply for an account"
            + "\n\t" + "exit = close program");
            switch (DaoFactory.getScanner().nextLine()) {
                case "customer": 
                    temp1 = askCustomerUsernamePassword();
                    if(temp1 == null) {
                        System.out.println("Unable to log in");
                    }
                    else {
                        acceptTransactions(temp1);
                        customerOptions(temp1);
                    }
                    temp1 = null;
                    restart = true;
                    // DaoFactory.closeScanner();
                    break;
                case "employee": 
                    temp2 = askEmployeeUsernamePassword();
                    if(temp2 == null) {
                        System.out.println("Unable to log in");
                    }
                    else {
                        employeeOptions(temp2);
                    }
                    temp2 = null;
                    restart = true;
                    // DaoFactory.closeScanner();
                    break;
                case "apply":
                    createNewCustomerAccount();
                    temp1 = null;
                    restart = true;
                    // DaoFactory.closeScanner();
                    break;
                case "exit": restart = false;
                    break;
                default: 
                    System.out.println("Invalid command");
                    restart = true;
                    // DaoFactory.closeScanner();
                    break;
            }
            temp1 = null;
            temp2 = null;
        } while (restart);
        DaoFactory.closeScanner();
        System.out.println("Bye bye!");
    }
    

}
