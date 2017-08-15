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

    public static void main(String argv[]) throws RiotApiException {

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
                    if (match.getQueue() == 4 && match.getSeason()==9 && check < 10) {
                        
                        Match thisMatch = api.getMatch(Platform.NA, match.getGameId());
                        String matchStr = String.valueOf(thisMatch.getGameId());
                        
                        
                        
                        Element matchElement = doc.createElement(Long.toString(thisMatch.getGameId()));
                        rootElement.appendChild(matchElement);
                        
                        Element winElement = doc.createElement("Result");
                        matchElement.appendChild(winElement);
                        int id = thisMatch.getParticipantByAccountId(summoner.getAccountId()).getTeamId();
                        winElement.setTextContent(thisMatch.getTeamByTeamId(id).getWin());
                        
                        Element champElement = doc.createElement("Champ");
                        matchElement.appendChild(champElement);
                        champElement.setTextContent(getChampName(String.valueOf(match.getChampion())));                     
                        
                        check++;
                    }
                }
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("C:\\Users\\james.raboin\\Desktop\\LoLRegMaster\\LoLRegression\\data\\sam.xml"));

            // Output to console for testing
            // StreamResult result = new StreamResult(System.out);
            transformer.transform(source, result);

            System.out.println("File saved!");

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }
    
    
    public static String getChampName(String id){
        
        String champName = "";
        
        try {

	File fXmlFile = new File("data\\champlist.xml");
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document doc = dBuilder.parse(fXmlFile);
	
	doc.getDocumentElement().normalize();

	System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

	NodeList nList = doc.getDocumentElement().getChildNodes();

	System.out.println("----------------------------");

	for (int temp = 0; temp < nList.getLength(); temp++) {

		Node nNode = nList.item(temp);

		System.out.println("\nCurrent Element :" + nNode.getNodeName());

		if (nNode.getNodeType() == Node.ELEMENT_NODE) {

			Element eElement = (Element) nNode;

			System.out.println("Champ: " + eElement.getAttribute("id"));
			
                        champName = nNode.getNodeName();
                        
                        if(id.equals(eElement.getAttribute("id")))
                            return champName;

		}
	}
    } catch (Exception e) {
	e.printStackTrace();
    }
        
        return champName;
    }

}
