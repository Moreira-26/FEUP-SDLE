package sdle.client;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import sdle.crdt.ORMap;

import java.util.ArrayList;

@Command(name = "shoppingList", description = "Shopping list application")
public class Cli implements Runnable {

    Controller controller;
    public Cli(Controller controller){
        this.controller = controller;
    }

    @Option(names = {"-u", "--username"}, description = "Username", required = true)
    private String username;

    @Option(names = {"-p", "--port"}, description = "Port", required = false)
    private int port;


    @Command(name = "push", description = "Push a shopping list to cloud")
    public void push(
            @Parameters(index = "0", description = "List ID") String listId
    ) {
        if(this.port == 0){
            System.out.println("Port not defined");
            return;
        }

        this.controller.config.setPort(port);
        this.controller.pushShoppingList(listId, username);
    }

    @Command(name = "pull", description = "Pull a shopping list from cloud")
    public void pull(
            @Parameters(index = "0", description = "List ID") String listId
    ) {
        if(this.port == 0){
            System.out.println("Port not defined");
            return;
        }
        this.controller.setUsername(username);
        this.controller.config.setPort(port);
        this.controller.pullShoppingList(listId, username);
    }

    @Command(name = "get", description = "Get a shopping list locally")
    public void get(
            @Parameters(index = "0", description = "List ID") String listId
    ) {

        ORMap shoppingList = controller.getShoppingList(listId, username);

        if (shoppingList != null){
            System.out.println(shoppingList.toStringPretty());
        }else{
            System.out.println("Shopping List " + listId + " doesnt exist\n");
        }
    }

    @Command(name = "create", description = "Create an empty shopping list locally")
    public void create(
            @Parameters(index = "0", description = "List ID") String listId
    ) {

        boolean created = controller.createShoppingList(listId, username);

        if(created) {
            System.out.println("Created list with ID: " + listId);
        }else{
            System.out.println("Error creating list or list already exists");
        }

    }
    @Command(name = "add", description = "Add an item to a shopping list locally")
    public void add(
            @Parameters(index = "0", description = "List ID") String listId,
            @Parameters(index = "1", description = "Item to add") String item,
            @Parameters(index = "2", description = "Quantity to add") int quantity
    ) {

        Boolean added = controller.addItemToList(listId, item,quantity, username);

        if(added) {
            System.out.println("Added item '" + item  +" with quantity " + quantity + "' to list with ID: " + listId);
        }else{
            System.out.println("Error adding item to shopping list");
        }
    }

    @Command(name = "remove", description = "Remove an item from a shopping list locally")
    public void remove(
            @Parameters(index = "0", description = "List ID") String listId,
            @Parameters(index = "1", description = "Item to remove") String item,
            @Parameters(index = "2", description = "Quantity") int quantity
    ) {
        Boolean removed = controller.removeItemFromList(listId,item,quantity,username);
        if(removed){
            System.out.println("Removing " + quantity + " of item '" + item + "' from list with ID: " + listId);
        }else{
            System.out.println("Error removing item from shopping list");
        }
    }

    @Command(name = "list", description = "List all shopping list ids")
    public void list(){
        ArrayList<String> listIds = controller.getShoppingListIds(username);
        if(listIds.isEmpty()){
            System.out.println("No shopping lists available");
        }else{
            System.out.println("Shopping Lists available:");
            System.out.println(listIds);
        }
    }


    @Override
    public void run() {
        // Default behavior when no subcommand is specified
        System.out.println("Please specify a subcommand (push, pull, get, add, remove).");
    }

    public static void main(String[] args) {
        Controller controller = new Controller();

        CommandLine app = new CommandLine(new Cli(controller));
        app.execute(args);
    }
}
