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

    public static void getMatches() throws RiotApiException {

        System.out.println("Connecting to MySQL Server...");

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC Driver Missing!");
            e.printStackTrace();
            return;
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

        try {

            DatabaseMetaData dbmd = connection.getMetaData();

            String[] types = {"TABLE"};
            ResultSet rs = dbmd.getTables(null, null, "%", types);
            while (rs.next()) {
                System.out.println(rs.getString("TABLE_NAME"));
            }
            
            
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ApiConfig config = new ApiConfig().setKey("RGAPI-9805073b-be47-4009-840e-b369abbd865a");
        RiotApi api = new RiotApi(config);

        //ChampionList champlist = api.getDataChampionList(Platform.NA);

        
        
        
        
        try {

            Summoner summoner = api.getSummonerByName(Platform.NA, "Canada Camera");
            MatchList matchList = api.getMatchListByAccountId(Platform.NA, summoner.getAccountId());
            
            String name = summoner.getName();

            String sql = "INSERT INTO `Summoners` VALUES('" + summoner.getAccountId() + "', '" + name + "');";
            
            System.out.println(sql);
                                           
            Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            
            connection.setAutoCommit(false);
            
            stmt.addBatch(sql);
            
            ResultSet rs = stmt.executeQuery("SELECT * FROM `Summoners`;");
            
            rs.last();
            
            System.out.println("rows before batch execution= "+ rs.getRow());
            
            stmt.executeBatch();
            connection.commit();
            
            System.out.println("Batch executed");
            rs = stmt.executeQuery("select * from Summoners");
            rs.last();
            System.out.println("rows after batch execution = "+ rs.getRow());
            
            
            int check = 0;
            
            if (matchList.getMatches() != null) {
                for (MatchReference match : matchList.getMatches()) {
                    if (check < 40) {

                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }

                        Match thisMatch = api.getMatch(Platform.NA, match.getGameId());
                        String matchStr = String.valueOf(thisMatch.getGameId());
                        int id = thisMatch.getParticipantByAccountId(summoner.getAccountId()).getTeamId();
                        Date expiry = new Date(thisMatch.getGameCreation());
                        String summonerFirstBloodKill = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstBloodKill());
                        String summonerFirstBloodAssist = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstBloodAssist());
                        String summonerFirstTowerKill = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstTowerKill());
                        String summonerFirstTowerAssist = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstTowerKill());
                        String teamFirstBlood = String.valueOf(thisMatch.getTeamByTeamId(id).isFirstBlood());
                        String teamFirstTower = String.valueOf(thisMatch.getTeamByTeamId(id).isFirstTower());

                        check++;

                        System.out.println("Game id: " + thisMatch.getGameId() + " saved!");
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
