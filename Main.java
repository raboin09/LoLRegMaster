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
        ApiConfig config = new ApiConfig().setKey("RGAPI-209eedb7-e302-439b-a950-4cd52deb1c1a");
        RiotApi api = new RiotApi(config);

        ChampionList champlist = api.getDataChampionList(Platform.NA);

        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("Games");
            doc.appendChild(rootElement);

            /*for(String key : champlist.getData().keySet()){
             Element champ = doc.createElement(key.toString());
             rootElement.appendChild(champ);
             champ.setAttribute("id", champlist.getData().get(key).toString().substring(champlist.getData().get(key).toString().indexOf("=") + 1, champlist.getData().get(key).toString().indexOf(":")));
             }*/
            
            Summoner summoner = api.getSummonerByName(Platform.NA, "Canada Camera");
            MatchList matchList = api.getMatchListByAccountId(Platform.NA, summoner.getAccountId());

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

                        Element matchElement = doc.createElement("Match");
                        rootElement.appendChild(matchElement);

                        Element winElement = doc.createElement("Result");
                        matchElement.appendChild(winElement);
                        int id = thisMatch.getParticipantByAccountId(summoner.getAccountId()).getTeamId();
                        winElement.setTextContent(thisMatch.getTeamByTeamId(id).getWin());

                        Element champElement = doc.createElement("Champ");
                        matchElement.appendChild(champElement);
                        champElement.setTextContent(getChampName(String.valueOf(match.getChampion())));
                                                
                        Date expiry = new Date( thisMatch.getGameCreation() );
                        Element dateElement = doc.createElement("Date");
                        matchElement.appendChild(dateElement);
                        dateElement.setTextContent(expiry.toString());
                        
                        String summonerFirstBloodKill = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstBloodKill());
                        String summonerFirstBloodAssist = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstBloodAssist());
                        String summonerFirstTowerKill = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstTowerKill());
                        String summonerFirstTowerAssist = String.valueOf(thisMatch.getParticipantByAccountId(summoner.getAccountId()).getStats().isFirstTowerKill());
                        String teamFirstBlood = String.valueOf(thisMatch.getTeamByTeamId(id).isFirstBlood());
                        String teamFirstTower = String.valueOf(thisMatch.getTeamByTeamId(id).isFirstTower());
                                               
                        Element snowballElement = doc.createElement("Snowball");
                        matchElement.appendChild(snowballElement);   
                        
                        Element summFbKill = doc.createElement("First_Blood_Kill");
                        snowballElement.appendChild(summFbKill);
                        summFbKill.setTextContent(summonerFirstBloodKill);
                        
                        Element summFbAssist = doc.createElement("First_Blood_Assist");
                        snowballElement.appendChild(summFbAssist);
                        summFbAssist.setTextContent(summonerFirstBloodAssist);
                        
                        Element summFtKill= doc.createElement("First_Tower_Kill");
                        snowballElement.appendChild(summFtKill);
                        summFtKill.setTextContent(summonerFirstTowerKill);
                        
                        Element summFtAssist = doc.createElement("First_Tower_Assist");
                        snowballElement.appendChild(summFtAssist);
                        summFtAssist.setTextContent(summonerFirstTowerAssist);
                        
                        Element teamFb = doc.createElement("Team_First_Blood");
                        snowballElement.appendChild(teamFb);
                        teamFb.setTextContent(teamFirstBlood);
                        
                        Element teamFt = doc.createElement("Team_First_Tower");
                        snowballElement.appendChild(teamFt);
                        teamFt.setTextContent(teamFirstTower);
                        
                        check++;
                        
                        System.out.println("Game id: " + thisMatch.getGameId() + " saved!");
                    }
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            
            StreamResult result = new StreamResult(new File("data\\sam.xml"));

            transformer.transform(source, result);

            System.out.println("File saved!");

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }

}
