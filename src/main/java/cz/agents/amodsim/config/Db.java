package cz.agents.amodsim.config;

import java.lang.String;

public final class Db {
  public final String password;

  public final String dbName;

  public final String user;

  public Db(String password, String dbName, String user) {
    this.password = password;
    this.dbName = dbName;
    this.user = user;
  }
}
