package lolregression;

import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.match.dto.MatchList;
import net.rithms.riot.api.endpoints.match.dto.MatchReference;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.api.endpoints.static_data.dto.ChampionList;
import net.rithms.riot.constant.Platform;
import net.rithms.riot.api.endpoints.match.dto.Match;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;
import java.time.Instant;
import java.time.ZoneId;

import java.util.Scanner;

import java.sql.*;
import java.util.Properties;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

public class Main {

    public static void main(String args[]) throws RiotApiException {

        getMatches();
    }

    public static String getChampName(String id) {

        String champName = "";

        try {

            File fXmlFile = new File("data\\champlist.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            NodeList nList = doc.getDocumentElement().getChildNodes();

            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    champName = nNode.getNodeName();

                    if (id.equals(eElement.getAttribute("id"))) {
                        return champName;
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return champName;
    }

    public static Connection dbConnect() {
        System.out.println("Connecting to MySQL Server...");

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver Missing!");
            e.printStackTrace();
        }

        Connection connection = null;

        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + "loldbmysql.crhbfyx7fdgr.us-east-2.rds.amazonaws.com" + ":" + "3306/mysqlloldb", "remoteu", "password");
        } catch (SQLException e) {
            System.out.println("Connection Failed!:\n" + e.getMessage());
        }

        if (connection != null) {
            System.out.println("Connection Successful!");
        } else {
            System.out.println("Failed to make connection!");
        }

        return connection;
    }

    public static void getMatches() throws RiotApiException {

        Scanner scan = new Scanner(System.in);

        System.out.println("Enter the summoner you'd like to retrieve: ");
        String input = scan.next();

        Connection posConnection = dbConnect();

        try {

            DatabaseMetaData dbmd = posConnection.getMetaData();

            String[] types = {"TABLE"};
            ResultSet rs = dbmd.getTables(null, null, "%", types);
            while (rs.next()) {
                System.out.println(rs.getString("TABLE_NAME"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        ApiConfig config = new ApiConfig().setKey("RGAPI-8c101364-a996-419c-8b07-617ab7ef833e");
        RiotApi api = new RiotApi(config);

        try {

            Summoner summoner = api.getSummonerByName(Platform.NA, input);
            long accountId = summoner.getAccountId();
            MatchList matchList = api.getMatchListByAccountId(Platform.NA, summoner.getAccountId());

            String name = summoner.getName();

            String sql = "CREATE TABLE IF NOT EXISTS `" + accountId + "`("
                    + "`summonerName` VARCHAR(45) NOT NULL,"
                    + " `summonerId` VARCHAR(45) NOT NULL,"
                    + " PRIMARY KEY(`summonerId`));";

            System.out.println(sql);

            Statement stmt = posConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

            posConnection.setAutoCommit(false);

            stmt.addBatch(sql);

            stmt.executeBatch();

            posConnection.commit();

            stmt.clearBatch();

            String sql1 = "IF NOT EXISTS (INSERT INTO `mysqlloldb`.`" + accountId + "` (summonerName, summonerId) VALUES('" + name + "', '" + accountId + "'));";

            stmt.addBatch(sql1);

            System.out.println(sql1);

            stmt.clearBatch();

            String sql3 = "INSERT INTO `AllSummoners`(summonerId, summonerName) "
                    + "SELECT * FROM (SELECT '" + accountId + "', '" + name + "')"
                    + " AS temp WHERE NOT EXISTS "
                    + "(SELECT summonerId FROM `AllSummoners` WHERE summonerId = '" + accountId + "') LIMIT 1";

            stmt.addBatch(sql3);

            System.out.println(sql3);

            //String sql2 = "INSERT INTO `AllSummoners`(summonerId, summonerName) VALUES ('" + accountId + "', '" + name + "');";
            //stmt.addBatch(sql2);
            //System.out.println(sql2);
            ResultSet rs = stmt.executeQuery("SELECT * FROM `AllSummoners`;");

            rs.last();

            System.out.println("rows before batch execution= " + rs.getRow());

            stmt.executeBatch();

            posConnection.commit();

            System.out.println("Batch executed");
            rs = stmt.executeQuery("select * from AllSummoners");
            rs.last();
            System.out.println("rows after batch execution = " + rs.getRow());

            int check = 0;

            if (matchList.getMatches() != null) {
                for (MatchReference match : matchList.getMatches()) {
                    if (check < 40 && match.getQueue() == 4) {

                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        Match thisMatch = api.getMatch(Platform.NA, match.getGameId());
                        String matchStr = String.valueOf(thisMatch.getGameId());
                        int particid = thisMatch.getParticipantByAccountId(summoner.getAccountId()).getTeamId();
                        String role = match.getRole();
                        Date expiry = new Date(thisMatch.getGameCreation());
                        String summonerFirstBloodKill = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstBloodKill());
                        String summonerFirstBloodAssist = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstBloodAssist());
                        String summonerFirstTowerKill = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstTowerKill());
                        String summonerFirstTowerAssist = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstTowerKill());
                        String teamFirstBlood = String.valueOf(thisMatch.getTeamByTeamId(particid).isFirstBlood());
                        String teamFirstTower = String.valueOf(thisMatch.getTeamByTeamId(particid).isFirstTower());

                        check++;

                        System.out.println("Game id: " + thisMatch.getGameId() + " saved!");

                        String matchSQLQuery = "CREATE TABLE IF NOT EXISTS `" + matchStr + "`("
                                + "`summoner1` VARCHAR(45) NOT NULL,"
                                + "`summoner2` VARCHAR(45) NOT NULL,"
                                + "`summoner3` VARCHAR(45) NOT NULL,"
                                + "`summoner4` VARCHAR(45) NOT NULL,"
                                + "`summoner5` VARCHAR(45) NOT NULL,"
                                + "`summoner6` VARCHAR(45) NOT NULL,"
                                + "`summoner7` VARCHAR(45) NOT NULL,"
                                + "`summoner8` VARCHAR(45) NOT NULL,"
                                + "`summoner9` VARCHAR(45) NOT NULL,"
                                + "`summoner10` VARCHAR(45) NOT NULL,"
                                + " PRIMARY KEY(`matchId`));";

                        stmt.addBatch(sql);

                        stmt.executeBatch();

                        posConnection.commit();

                        stmt.clearBatch();

                        System.out.println(sql);
                    }
                }
            }

            System.out.println("Record saved!");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void dbConnect(String userName, String password, String url) {

    }

}
