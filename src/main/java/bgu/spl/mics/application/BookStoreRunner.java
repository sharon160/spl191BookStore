package bgu.spl.mics.application;

import bgu.spl.mics.application.passiveObjects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class BookStoreRunner {
    public static void main(String[] args) {
        JsonParser parser = new JsonParser();
        Gson gson = new Gson();
        try {
            JsonObject object=(JsonObject) parser.parse(new FileReader(args[0]));
            JsonArray array = object.get("initialInventory").getAsJsonArray();
            BookInventoryInfo[] bookInventory = gson.fromJson(array, BookInventoryInfo[].class);
            Inventory inventory = Inventory.getInstance();
            inventory.load(bookInventory);
            array = object.get("initialResources").getAsJsonArray().get(0).getAsJsonObject().get("vehicles").getAsJsonArray();
            DeliveryVehicle[] deliveryVehicles = gson.fromJson(array, DeliveryVehicle[].class);
            ResourcesHolder resourcesHolder = ResourcesHolder.getInstance();
            resourcesHolder.load(deliveryVehicles);
            JsonObject jsonObject = object.get("services").getAsJsonObject();
            Customer[] customers = gson.fromJson(jsonObject.get("customers").getAsJsonArray(), Customer[].class);
            List<Thread> threads = new LinkedList<>();
            initialServices(jsonObject, threads, customers);
            HashMap<Integer, Customer> customerHashMap = new HashMap<>();
            for(Customer customer : customers)
                customerHashMap.put(customer.getId(), customer);

            serializeObject(customerHashMap, args[1]);
            inventory.printInventoryToFile(args[2]);
            MoneyRegister.getInstance().printOrderReceipts(args[3]);
            serializeObject(MoneyRegister.getInstance(), args[4]);
            System.out.println(MoneyRegister.getInstance().getTotalEarnings());




        }
        catch (FileNotFoundException e) {

        }
    }

    private static void initialServices(JsonObject jsonObject, List<Thread> threads, Customer[] customers) {
        int numberOfSellingServices = jsonObject.get("selling").getAsInt();
        int numberOfInventoryServices = jsonObject.get("inventoryService").getAsInt();
        int numberOfLogisticsServices = jsonObject.get("logistics").getAsInt();
        int numberOfResourcesServices = jsonObject.get("resourcesService").getAsInt();
        CountDownLatch countDownLatch = new CountDownLatch(numberOfSellingServices + numberOfInventoryServices + numberOfResourcesServices + numberOfLogisticsServices + customers.length);
        for (int i=0; i < numberOfResourcesServices; i++)
            threads.add(new Thread(new ResourceService("ResourceService" + i, countDownLatch)));
        for (int i=0; i < customers.length; i++)
            threads.add(new Thread(new APIService("APIService" + i, customers[i], countDownLatch)));
        for(int i = 0; i < numberOfSellingServices; i++)
            threads.add(new Thread(new SellingService("SellingService" + i, countDownLatch)));
        for(int i = 0; i < numberOfInventoryServices; i++)
            threads.add(new Thread(new InventoryService("InventoryService" + i, countDownLatch)));
        for (int i=0; i < numberOfLogisticsServices; i++)
            threads.add(new Thread(new LogisticsService("LogisticsService" + i,countDownLatch)));

        for(Thread t : threads)
            t.start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int speed = jsonObject.get("time").getAsJsonObject().get("speed").getAsInt();
        int duration = jsonObject.get("time").getAsJsonObject().get("duration").getAsInt();
        Thread timeService = new Thread(new TimeService(speed, duration));
        timeService.start();
        try{
            for(Thread t : threads)
                t.join();
            timeService.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void serializeObject(Serializable s,String filename) {
        try {
            FileOutputStream fileOut =new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(s);
            out.close();
            fileOut.close();

        } catch (IOException i) {
            i.printStackTrace();
        }
    }
}
