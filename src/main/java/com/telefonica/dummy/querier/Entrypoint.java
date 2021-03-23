package com.telefonica.dummy.querier;


import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class Entrypoint {

  private void populateDb(Connection conn) {
    try {
      Statement st = conn.createStatement();

      st.addBatch("DROP TABLE IF EXISTS friends");
      st.addBatch("CREATE TABLE friends(id serial, name TEXT)");
      st.addBatch("INSERT INTO friends(name) VALUES ('Guido')");
      st.addBatch("INSERT INTO friends(name) VALUES ('David')");
      st.addBatch("INSERT INTO friends(name) VALUES ('Diego')");
      st.addBatch("INSERT INTO friends(name) VALUES ('Pedro')");
      st.addBatch("INSERT INTO friends(name) VALUES ('Manuel')");

      int counts[] = st.executeBatch();

      conn.commit();

      System.out.println("Successfully created and populated friends table with " + counts.length);

    } catch (SQLException ex) {
      ex.printStackTrace();
    }
  }

  private boolean queryData(Connection conn) {
    if (conn == null) {
      return false;
    }
    try {
      Statement st = conn.createStatement();
      ResultSet rs = st.executeQuery("SELECT * from friends");
      String lastName = "";
      while (rs.next()) {
        lastName = rs.getString(2);
      }
      System.out.println("Last name read is " + lastName);
    } catch (SQLException ex) {
      ex.printStackTrace();
      return false;
    }
    return true;
  }

  private boolean addData(Connection conn) {
    if (conn == null) {
      return false;
    }
    try {
      String name = UUID.randomUUID().toString();

      Statement st = conn.createStatement();
      st.executeUpdate(String.format("INSERT INTO friends(name) VALUES ('%s')", name));
      conn.commit();
    } catch (SQLException ex) {
      ex.printStackTrace();
      if (conn != null) {
        try {
          conn.rollback();
        } catch (SQLException ex1) {
          System.out.println("Can not rollback");
          ex1.printStackTrace();
        }
      }
      return false;
    }
    return true;
  }

  private Connection getConnection(String url, String user, String password) {
    Connection conn = null;
    try {
      conn = DriverManager.getConnection(url, user, password);
      conn.setAutoCommit(false);
    } catch (SQLException e) {
      System.out.println("Can not create a new connection");
      e.printStackTrace();
    }

    return conn;
  }

  public static void main(String[] args) {
    String url = Optional.ofNullable(System.getenv("JDBC_URL"))
        .orElse("jdbc:postgresql://localhost:5432/oic?socketTimeout=10");
    String user = Optional.ofNullable(System.getenv("DB_USERNAME"))
        .orElse("oic");
    String password = Optional.ofNullable(System.getenv("DB_PASSWORD"))
        .orElse("oic");
    int sleepTime = Optional.ofNullable(System.getenv("SLEEP_TIME"))
        .map(Integer::parseInt)
        .orElse(5000);
    int maxLifeTime = Optional.ofNullable(System.getenv("MAXLIFE_TIME"))
        .map(Integer::parseInt)
        .orElse(30000);
    int whenWrite = 10;

    System.out.println(String.format("url %s, user %s, pass %s, sleepTime %d, maxLifeTime %d", url, user, password,
        sleepTime, maxLifeTime));

    Entrypoint entrypoint = new Entrypoint();
    Connection conn;

    try {
      conn = entrypoint.getConnection(url, user, password);
      entrypoint.populateDb(conn);
      int numReads = 0;
      boolean connectionAvailable = true;
      long connTime = System.currentTimeMillis();
      while (true) {
        System.out.println("Read data from database");
        connectionAvailable = entrypoint.queryData(conn);
        numReads++;

        Thread.currentThread().sleep(sleepTime);

        if (numReads > whenWrite) {
          System.out.println("*** Write some data to database ***");
          connectionAvailable = entrypoint.addData(conn);
          numReads = 0;
        }

        if (!connectionAvailable) {
          System.out.println("--- Connection is not available, trying to renew!!!! ---");
          if (conn == null || conn.isClosed()) {
            System.out.println("The connection has been closed");
          } else {
            System.out.println("The connection is still opened!!!!!!!!");
            conn.close();
          }
          conn = entrypoint.getConnection(url, user, password);
          System.out.println("The connection has been tried to be reopened after not available");
          connTime = System.currentTimeMillis();
        } else if ((System.currentTimeMillis() - connTime) > maxLifeTime) {
          System.out.println("--- Connection has passed maxLifeTime, renewing ---");
          if (conn != null) {
            conn.close();
          }

          conn = entrypoint.getConnection(url, user, password);
          System.out.println("The connection has been tried to be reopened");
          connTime = System.currentTimeMillis();
        }
      }
    } catch (SQLException ex) {
      ex.printStackTrace();
      System.exit(1);
    } catch (InterruptedException e) {
      System.out.println("Stopped execution");
    }
  }
}
