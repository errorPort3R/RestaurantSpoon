package com.theironyard.javawithclojure.jhporter;

import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
    static HashMap<String, User> users = new HashMap<>();

    public static void main(String[] args) throws SQLException
    {
        Spark.staticFileLocation("/public");
        Spark.init();

        boolean search = false;

        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS restaurants (id IDENTITY, name VARCHAR, location VARCHAR, rating INT, comment VARCHAR)");


        Spark.get(
                "/",
                (request, response) ->
                {
                    Session session = request.session();
                    String username = session.attribute("username");

                    HashMap m = new HashMap();
                    if (username == null)
                    {
                        return new ModelAndView(m, "login.html");
                    }
                    else
                    {
                        User user = users.get(username);
                        m.put("restaurants", selectRestaurants(conn));
                        return new ModelAndView(m, "home.html");
                    }

                },
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/login",
                (request, response) -> {
                    String name = request.queryParams("username");
                    String pass = request.queryParams("password");
                    if (name == null || pass == null)
                    {
                        throw new Exception("Name or pass not sent");
                    }

                    User user = users.get(name);
                    if (user == null)
                    {
                        user = new User(name, pass);
                        users.put(name, user);
                    } else if (!pass.equals(user.name))
                    {
                        throw new Exception("Wrong password");
                    }

                    Session session = request.session();
                    session.attribute("username", name);

                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/create-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");

                    if (username == null)
                    {
                        throw new Exception("Not Logged in!");
                    }

                    String name = request.queryParams("name");
                    String location = request.queryParams("location");
                    int rating = Integer.valueOf(request.queryParams("rating"));
                    String comment = request.queryParams("comment");
                    if (name == null || location == null || comment == null)
                    {
                        throw new Exception("Invalid Fields!");
                    }

                    User user = users.get(username);
                    if (user == null)
                    {
                        throw new Exception("User does not exist!");
                    }
                    Restaurant r = new Restaurant(name, location, rating, comment);
                    insertRestaurant(conn, r);

                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/delete-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    int id = Integer.valueOf(request.queryParams("id"));
                    deleteRestaurant(conn, id);
                    response.redirect("/");
                    return "";
                }
        );
        Spark.get(
                "/edit-restaurant",
                (request, response) ->
                {
                    Session session = request.session();
                    String username = session.attribute("username");
                    int id = Integer.valueOf(request.queryParams("id"));
                    HashMap h = new HashMap<>();
                    h.put("restaurant", getRestaurant(conn, id));
                    return new ModelAndView(h, "edit.html");

                },
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/update-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    int id = Integer.valueOf(request.queryParams("id"));
                    String name = request.queryParams("name");
                    String location = request.queryParams("location");
                    int rating = Integer.valueOf(request.queryParams("rating"));
                    String comment = request.queryParams("comment");
                    if (name == null || location == null || comment == null)
                    {
                        throw new Exception("Invalid Fields!");
                    }
                    Restaurant restaurant = new Restaurant(id, name, location, rating, comment);
                    updateRestaurant(conn, restaurant);

                    response.redirect("/");
                    return "";
                }
        );
        Spark.get(
                "/search-names",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    String searchString = request.queryParams("search");
                    HashMap hm = new HashMap<>();

                    hm.put("restaurants", searchNames(conn, searchString));
                    return new ModelAndView(hm, "home.html");

                },
                new MustacheTemplateEngine()
        );
    }

    public static void insertRestaurant(Connection conn, Restaurant restaurant) throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO restaurants VALUES(NULL,?,?,?,?)");
        stmt.setString(1,restaurant.name);
        stmt.setString(2,restaurant.location);
        stmt.setInt(3,restaurant.rating);
        stmt.setString(4,restaurant.comment);
        stmt.execute();
    }

    public static void deleteRestaurant(Connection conn, int id) throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM restaurants WHERE id = ?");
        stmt.setInt(1,id);
        stmt.execute();
    }

    public static ArrayList<Restaurant> selectRestaurants(Connection conn) throws SQLException
    {
        ArrayList<Restaurant> restaurantList= new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants");
        ResultSet results = stmt.executeQuery();
        ArrayList<Restaurant> restaurants = new ArrayList<>();
        while (results.next())
        {
            int id = results.getInt("id");
            String name = results.getString("name");
            String location = results.getString("location");
            int rating = results.getInt("rating");
            String comment = results.getString("comment");
            Restaurant restaurant = new Restaurant(id,name,location,rating,comment);
            restaurants.add(restaurant);
        }
        return restaurants;
    }

    public static void updateRestaurant(Connection conn, Restaurant restaurant) throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement("UPDATE restaurants SET name = ? , location = ?, rating = ?, comment = ? WHERE id = ?");
        stmt.setString(1,restaurant.name);
        stmt.setString(2,restaurant.location);
        stmt.setInt(3,restaurant.rating);
        stmt.setString(4,restaurant.comment);
        stmt.setInt(5,restaurant.id);
        stmt.execute();
    }

    public static Restaurant getRestaurant(Connection conn, int id) throws SQLException
    {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants WHERE id = ?");
        stmt.setInt(1,id);
        ResultSet results = stmt.executeQuery();
        Restaurant restaurant = null;
        while (results.next())
        {
            int newId = results.getInt("id");
            String name = results.getString("name");
            String location = results.getString("location");
            int rating = results.getInt("rating");
            String comment = results.getString("comment");
            restaurant = new Restaurant(newId,name,location,rating,comment);
        }
        return restaurant;
    }

    public static ArrayList<Restaurant> searchNames(Connection conn, String search) throws SQLException
    {
        ArrayList<Restaurant> restaurantList= new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants");
        ResultSet results = stmt.executeQuery();
        ArrayList<Restaurant> restaurants = new ArrayList<>();
        while (results.next())
        {
            int id = results.getInt("id");
            String name = results.getString("name");
            String location = results.getString("location");
            int rating = results.getInt("rating");
            String comment = results.getString("comment");
            Restaurant restaurant = new Restaurant(id,name,location,rating,comment);
            search = search.toLowerCase();
            String lowerName = name.toLowerCase();
            if (lowerName.contains(search))
            {
                restaurants.add(restaurant);
            }
        }
        return restaurants;
    }
}
