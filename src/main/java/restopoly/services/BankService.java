package restopoly.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpStatus;
import restopoly.resources.Account;
import restopoly.resources.Bank;
import restopoly.resources.Event;
import restopoly.resources.Player;
import restopoly.util.Ports;
import restopoly.util.Service;

import java.util.ArrayList;

import static restopoly.util.Ports.BANKSADDRESS;
import static spark.Spark.*;

/**
 * Created by Krystian.Graczyk on 05.11.15.
 */
public class BankService {

    public static void main(String[] args) {
//TODO - später auskommentieren
        port(4569);

        BankService bankService = new BankService();

        ArrayList<Bank> banks = new ArrayList<>();

        get("/banks/:gameid", (req, res) -> {
            res.header("Content-Type", "application/json");
            res.status(200);
            Bank bank = null;
            for(Bank b : banks){
                if(b.getGameid().equals(req.params(":gameid"))) bank=b;
            }
            Gson gson = new GsonBuilder()
                    .create();
            return gson.toJson(bank);
        });

        put("/banks/:gameid", (req, res) -> {
            res.header("Content-Type", "application/json");
            res.status(HttpStatus.SC_OK);
            res.header(Ports.GAME_KEY, req.headers(Ports.GAME_KEY));
            res.header(Ports.DICE_KEY, req.headers(Ports.DICE_KEY));
            res.header(Ports.BANK_KEY, req.headers(Ports.BANK_KEY));
            res.header(Ports.BOARD_KEY, req.headers(Ports.BOARD_KEY));
            res.header(Ports.EVENT_KEY, req.headers(Ports.EVENT_KEY));
            res.header(Ports.BROOKER_KEY, req.headers(Ports.BROOKER_KEY));

            Bank bank = new Bank(req.params(":gameid"));
            banks.add(bank);
            Gson gson = new GsonBuilder()
                    .create();
            return gson.toJson(bank);
        });

        post("/banks/:gameid/players", (req, res) -> {
            res.status(HttpStatus.SC_OK);
            res.header("Content-Type", "application/json");
            res.header(Ports.GAME_KEY, req.headers(Ports.GAME_KEY));
            res.header(Ports.DICE_KEY, req.headers(Ports.DICE_KEY));
            res.header(Ports.BANK_KEY, req.headers(Ports.BANK_KEY));
            res.header(Ports.BOARD_KEY, req.headers(Ports.BOARD_KEY));
            res.header(Ports.EVENT_KEY, req.headers(Ports.EVENT_KEY));
            res.header(Ports.BROOKER_KEY, req.headers(Ports.BROOKER_KEY));
            Bank bank = null;
            for(Bank b : banks){
                if(b.getGameid().equals(req.params(":gameid"))) bank=b;
            }

//            TODO - Änderungen vorgenommen, da laut Specs ein Player als Body übergeben wird!
            Gson gsonTempPlayer = new Gson();
            Player tempPlayer =  gsonTempPlayer.fromJson(req.body().toString(), Player.class);

            Gson gson = new Gson();
//            if(bank.getAccount(req.body())!=null){
            if(bank.getAccount(tempPlayer.getId())!=null){
                res.status(409);
                return gson.toJson(bank.getAccount(req.body()));
            }

//            HttpResponse playerResponse = Unirest.get(Ports.GAMESADDRESS + "/"+ req.params(":gameid")+"/players/"+req.body()).asJson();
            HttpResponse playerResponse = Unirest.get(Ports.GAMESADDRESS + "/"+ req.params(":gameid")+"/players/"+tempPlayer.getId()).asJson();
            Player player = gson.fromJson(playerResponse.getBody().toString(), Player.class);

//            Account account = new Account(req.body(),player,0);
            Account account = new Account(player.getId(),player,0);
            bank.addAccount(account);
            return gson.toJson(account);
        });

        get("/banks/:gameid/players", (req, res) -> {
            res.status(200);
            res.header("Content-Type", "application/json");
            Bank bank = null;
            for(Bank b : banks){
                if(b.getGameid().equals(req.params(":gameid"))) bank=b;
            }
            Gson gson = new GsonBuilder().create();
            return gson.toJson(bank.getAccounts());
        });

        get("/banks/:gameid/players/:playerid", (req, res) -> {
            res.status(200);
            res.header("Content-Type", "application/json");
            Bank bank = null;
            for(Bank b : banks){
                if(b.getGameid().equals(req.params(":gameid"))) bank=b;
            }
            int saldo = bank.getAccount(req.params(":playerid")).getSaldo();
            Gson gson = new GsonBuilder().create();
            return gson.toJson(saldo);
        });

//        TODO - einbauen
//        req.Body() = {"type": "string",
//                "required": true,
//                "description": "the reason for the transfer"}
//        Response = a List of Events
        post("/banks/:gameid/transfer/to/:to/:amount", (req, res) -> {
            synchronized(bankService) {
                res.status(201);
                res.header("Content-Type", "application/json");
                Bank bank = null;
                for (Bank b : banks) {
                    if (b.getGameid().equals(req.params(":gameid"))) bank = b;
                }
                Account to = bank.getAccount(req.params(":to"));
                int saldo = to.getSaldo();
                try {
                    to.setSaldo(saldo + Integer.parseInt(req.params(":amount")));

//                    Gson gson = new GsonBuilder()
//                            .setExclusionStrategies(new CustomExclusionStrategy("restopoly.resources.Account.player"))
//                            .create();
                    Event event = new Event("type","transfer to amount","resource","reason",to.getPlayer());
                    Unirest.post(restopoly.util.Ports.EVENTSADDRESS).queryString("gameid", req.params(":gameid")).body(new Gson().toJson(event)).asString();
                    HttpResponse tempEvents = Unirest.get(restopoly.util.Ports.EVENTSADDRESS).asString();
                    Gson gsonEvents = new Gson();
                    Event[] resultEvents = gsonEvents.fromJson(tempEvents.getBody().toString(), Event[].class);
                    return gsonEvents.toJson(resultEvents);
                } catch (Throwable e) {
                    to.setSaldo(saldo);
                    res.status(500);
                    return "";
                }
            }
        });

//        TODO - einbauen
//        req.Body() = {"type": "string",
//                "required": true,
//                "description": "the reason for the transfer"}
//        Response = a List of Events
        post("/banks/:gameid/transfer/from/:from/:amount", (req, res) -> {
            synchronized(bankService) {
                res.status(201);
                res.header("Content-Type", "application/json");
                Bank bank = null;
                for (Bank b : banks) {
                    if (b.getGameid().equals(req.params(":gameid"))) bank = b;
                }
                Account from = bank.getAccount(req.params(":from"));
                if (from.getSaldo() < Integer.parseInt(req.params(":amount"))) {
                    res.status(403);
                    return "";
                }
                int saldo = from.getSaldo();
                try {
                    from.setSaldo(saldo - Integer.parseInt(req.params(":amount")));

//                    Gson gson = new GsonBuilder()
//                            .setExclusionStrategies(new CustomExclusionStrategy("restopoly.resources.Account.player"))
//                            .create();
                    Event event = new Event("type","transfer from amount","resource","reason",from.getPlayer());
                    Unirest.post(restopoly.util.Ports.EVENTSADDRESS).queryString("gameid", req.params(":gameid")).body(new Gson().toJson(event)).asString();
                    HttpResponse tempEvents = Unirest.get(restopoly.util.Ports.EVENTSADDRESS).asJson();
                    Gson gsonEvents = new Gson();
                    Event[] resultEvents = gsonEvents.fromJson(tempEvents.getBody().toString(), Event[].class);
                    return gsonEvents.toJson(resultEvents);
                } catch (Throwable e) {
                    from.setSaldo(saldo);
                    res.status(500);
                    return "";
                }
            }
        });

//        TODO - einbauen
//        req.Body() = {"type": "string",
//                "required": true,
//                "description": "the reason for the transfer"}
//        Response = a List of Events
        post("/banks/:gameid/transfer/from/:from/to/:to/:amount", (req, res) -> {
            synchronized(bankService) {
                res.status(201);
                res.header("Content-Type", "application/json");
                Bank bank = null;
                for (Bank b : banks) {
                    if (b.getGameid().equals(req.params(":gameid"))) bank = b;
                }
                Account from = bank.getAccount(req.params(":from"));
                int fromsaldo = from.getSaldo();

                Account to = bank.getAccount(req.params(":to"));
                int tosaldo = to.getSaldo();

                if (fromsaldo < Integer.parseInt(req.params(":amount"))) {
                    res.status(403);
                    return "";
                }
                try {
                    from.setSaldo(fromsaldo - Integer.parseInt(req.params(":amount")));
                    to.setSaldo(tosaldo + Integer.parseInt(req.params(":amount")));
                    Event event = new Event("type","transfer from to amount","resource","reason",to.getPlayer());
                    Unirest.post(restopoly.util.Ports.EVENTSADDRESS).queryString("gameid", req.params(":gameid")).body(new Gson().toJson(event)).asString();
                    HttpResponse tempEvents = Unirest.get(restopoly.util.Ports.EVENTSADDRESS).asString();
                    Gson gsonEvents = new Gson();
                    Event[] resultEvents = gsonEvents.fromJson(tempEvents.getBody().toString(), Event[].class);
                    return gsonEvents.toJson(resultEvents);
//                    return "";
                } catch (Throwable e) {
                    from.setSaldo(fromsaldo);
                    to.setSaldo(tosaldo);
                    res.status(500);
                    return "";
                }
            }
        });


        try {
            Unirest.post("http://vs-docker.informatik.haw-hamburg.de:8053/services")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .queryString("name", "BANKS")
                    .queryString("description", "Banks Service")
                    .queryString("service", "banks")
                    .queryString("uri", BANKSADDRESS)
                    .body(new Gson().toJson(new Service("BANKS", "Banks Service", "banks", BANKSADDRESS)))
                    .asJson();
        } catch (UnirestException e) {
            e.printStackTrace();

        }

    }
}

