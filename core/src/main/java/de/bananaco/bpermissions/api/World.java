package de.bananaco.bpermissions.api;

import java.util.*;
import java.util.regex.Pattern;

import com.evilmidget38.UUIDFetcher;
import de.bananaco.bpermissions.util.Debugger;

/**
 * This is the class to extend for new implementations of bPermissions.
 *
 * With this class, other ways to load/save permissions will become easily
 * available (hopefully)...
 */
public abstract class World {

    private final Map<String, Group> groups;
    private final Map<String, User> users;
    private final String world;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public World(String world) {
        this.world = world;
        this.users = new HashMap();
        this.groups = new HashMap();
    }

    /**
     * Make sure you call .calculateEffectivePermissions for all the users once
     * this is done!
     *
     * You can just call add(Calculable) here with the objects you create.
     *
     * @return boolean
     */
    public abstract boolean load();

    /**
     * This should be as efficient as possible, can even be threaded if you
     * really desire. This is an attempt to increase compatibility with
     * everything!
     *
     * @return boolean
     */
    public abstract boolean save();

    /**
     * This loads a single Calculable into the API
     *
     * Make sure you call .calculateEffectivePermissions for all the users once
     * this is done!
     *
     * You can just call add(Calculable) here with the objects you create.
     *
     * @return boolean
     */
    public abstract boolean loadOne(String name, CalculableType type);

    /**
     * This saves a single Calculable into the storage
     *
     * This should be as efficient as possible, can even be threaded if you
     * really desire. This is an attempt to increase compatibility with
     * everything!
     *
     * [REMOVED FOR NOW - UNUSED]
     *
     * @return boolean
     * public abstract boolean saveOne(String name, CalculableType type);
     */

    /**
     * This is a way of checking whether a Calculable is stored within
     * the storage that this world uses.
     *
     * Will return true if the store cointains the Calculable
     *
     * @return boolean
     */
    public abstract boolean storeContains(String name, CalculableType type);


    /**
     * Used to check if the World contains an entry for said Calculable
     *
     * @param name
     * @param type
     * @return boolean
     */
    public boolean contains(String name, CalculableType type) {
        name = stripColor(name);
        // A quick lowercase here
        name = name.toLowerCase();
        // And now we check
        if (type == CalculableType.USER) {
            return users.containsKey(name);
        } else if (type == CalculableType.GROUP) {
            return groups.containsKey(name);
        }
        return false;
    }

    public Group getGroup(String name) {
        name = stripColor(name);
        return (Group) get(name, CalculableType.GROUP);
    }

    @Deprecated
    public User getUser(String name) {
        name = stripColor(name);
        return (User) get(name, CalculableType.USER);
    }

    public User getUser(UUID uuid) {
        return (User) get(uuid.toString(), CalculableType.USER);
    }

    /**
     * Used to get the contained Calculable (contains should be used first)
     *
     * @param name
     * @param type
     * @return Calculable (Group/User)
     */
    public Calculable get(String name, CalculableType type) {
        name = stripColor(name);
        // A quick lowercase here
        name = name.toLowerCase();
        // laod the Calculable if it isn't already
        if (storeContains(name, type) && !contains(name, type)) {
            loadOne(name, type);
        }

        if (type == CalculableType.USER) {
            if (!isUUID(name))
                name = getUUID(name).toString();

            if (!users.containsKey(name)) {
                add(new User(name, null, null, getName(), this));
                // Don't forget to add the default group!
                users.get(name).addGroup(getDefaultGroup());
                // And calculate the effective Permissions!
                try {
                    users.get(name).calculateEffectivePermissions();
                    users.get(name).calculateEffectiveMeta();
                } catch (RecursiveGroupException e) {
                    System.err.println(e.getMessage());
                }
            }
            return users.get(name);
        } else if (type == CalculableType.GROUP) {
            if (!groups.containsKey(name)) {
                add(new Group(name, null, null, getName(), this));
            }
            return groups.get(name);
        }
        return null;
    }

    /**
     * Used to grab a complete set of the contained Calculable from the World.
     * Should never return null but may return an empty Set<Calculable>
     * Returns a new Set with direct references to the object.
     *
     * @param type
     * @return Set<Calculable>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Set<Calculable> getAll(CalculableType type) {
        Set<Calculable> entries = new HashSet();
        Map<String, Calculable> data = getAllAsMap(type);
        // catch null
        if (data == null) {
            return null;
        }
        // And now we grab
        if (type == CalculableType.USER) {
            for (String key : data.keySet()) {
                entries.add(data.get(key));
            }
            return entries;
        } else if (type == CalculableType.GROUP) {
            for (String key : data.keySet()) {
                entries.add(data.get(key));
            }
            return entries;
        }
        data.clear();
        return entries;
    }

    private Map<String, Calculable> getAllAsMap(CalculableType type) {
        try {
            if (type == CalculableType.USER) {
                return new HashMap(users);
            } else if (type == CalculableType.GROUP) {
                return new HashMap(groups);
            }
        } catch (Exception e) {
            Debugger.log("Error getting " + type.name());
        }
        return null;
    }

    /**
     * This adds the Calculable to either groups or users depending on if the
     * calculable is an instance of either. This is not directly checked and
     * instead getType() is relied upon to be correct. If the calculable is not
     * an instance of a group or a user, it is not added. This means you cannot
     * add base calculables (or any other class which extends calculable) to
     * this.
     *
     * @param calculable
     */
    public void add(Calculable calculable) {
        if (calculable.getType() == CalculableType.USER) {
            users.put(calculable.getNameLowerCase(), (User) calculable);
        } else if (calculable.getType() == CalculableType.GROUP) {
            groups.put(calculable.getNameLowerCase(), (Group) calculable);
        } else {
            System.err.println("Calculable not instance of User or Group!");
        }
    }

    /**
     * This removes the Calculable from either groups or users depending on if the
     * calculable is an instance of either. This is not directly checked and
     * instead getType() is relied upon to be correct. If the calculable is not
     * an instance of a group or a user, it is not added. This means you cannot
     * add base calculables (or any other class which extends calculable) to
     * this.
     *
     * @param calculable
     */
    public void remove(Calculable calculable) {
        if (calculable.getType() == CalculableType.USER) {
            users.remove(calculable.getNameLowerCase());
        } else if (calculable.getType() == CalculableType.GROUP) {
            groups.remove(calculable.getNameLowerCase());
        } else {
            System.err.println("Calculable not instance of User or Group!");
        }
    }

    /**
     * Returns the world name
     *
     * @return String
     */
    public String getName() {
        return world;
    }

    /**
     * Used to clear the Maps containing User and Group object (useful for doing
     * a clean load)
     */
    public void clear() {
        //for(Calculable group : getAll(CalculableType.GROUP)) {
        //	group.clear();
        //}
        //for(Calculable user : getAll(CalculableType.USER)) {
        //	user.clear();
        //}
        groups.clear();
        users.clear();
    }

    /**
     * Shows if the world is THIS world
     *
     * @param world
     * @return boolean
     */
    public boolean equalsWorld(String world) {
        return world.equalsIgnoreCase(this.world);
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        return o.hashCode() == hashCode();
    }

    /**
     * This is the implementation of the .cleanup() in WorldManager
     *
     * Removes any empty groups and any users with just the default group
     */
    protected void cleanup() {
        List<String> removal = new ArrayList<String>();
        // Iterate through the users
        for (String user : users.keySet()) {
            User u = users.get(user);


            if (u.getMeta().size() == 0
                    && u.getPermissions().size() == 0
                    && (u.getGroupsAsString().size() == 0
                    || (u.getGroupsAsString().size() == 1
                    && u.getGroupsAsString().iterator().next().equals(getDefaultGroup())))) {
                removal.add(user);
            }
        }
        // Remove the user if it's been flagged
        for (String user : removal) {
            users.remove(user);
        }
        removal.clear();
        // Iterate through the groups
        for (String group : groups.keySet()) {
            Group g = groups.get(group);
            if (g.getMeta().size() == 0
                    && g.getPermissions().size() == 0
                    && g.getGroupsAsString().size() == 0) {
                removal.add(group);
            }
        }
        // Remove the group if it's been flagged
        for (String group : removal) {
            groups.remove(group);
        }
        // And finally save the cleaned up files
        save();
    }

    public abstract void setDefaultGroup(String group);

    public abstract String getDefaultGroup();

    public abstract boolean setupPlayer(String player);

    public boolean setupAll() {
        // override to return true
        return false;
    }

    public boolean isOnline(User user) {
        // override to return true;
        return false;
    }

    public void clearPlayers() {
        users.clear();
    }


    public boolean isUUID(String string) {
        try {
            UUID.fromString(string);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Abstract method to get a players UUID given their username using whatever
     * method is used in the implementation of a World
     *
     * @param player The name of the Player
     * @return uuid The UUID of the Player
     */
    public abstract UUID getUUID(String player);


    /**
     * Strips the given message of all color codes - taken from Bukkit source code
     *
     * @param input String to strip of color
     * @return A copy of the input string, without any coloring
     */
    public static String stripColor(final String input) {
        if (input == null) {
            return null;
        }
        char COLOR_CHAR = '\u00A7';
        Pattern stripColorPattern = Pattern.compile("(?i)" + String.valueOf(COLOR_CHAR) + "[0-9A-FK-OR]");

        return stripColorPattern.matcher(input).replaceAll("");
    }

}
