import java.io.*;
import java.util.*;

/**
 * Contact Management System - core Java + Data Structures
 *
 * DSA used:
 *  1. HashMap  -> find / add / delete contact by phone in O(1)
 *  2. Trie     -> name prefix search (auto-suggest) in O(L + result)
 *  3. Merge Sort (own implementation) -> sorted display, O(n log n)
 *  4. Stack    -> undo last delete, O(1)
 *  5. ArrayList -> dynamic list of contacts
 */
public class ContactManagementSystem {

    // ---------------- Contact ----------------
    static class Contact {
        String name, phone, email, address;

        Contact(String name, String phone, String email, String address) {
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.address = address;
        }

        @Override
        public String toString() {
            return String.format("%-20s %-12s %-25s %s", name, phone, email, address);
        }
    }

    // ---------------- Trie (name prefix search) ----------------
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        List<String> phones = new ArrayList<>(); // phones of contacts whose name ends here
    }

    static class Trie {
        private final TrieNode root = new TrieNode();

        void insert(String name, String phone) {
            TrieNode cur = root;
            for (char ch : name.toLowerCase().toCharArray()) {
                cur = cur.children.computeIfAbsent(ch, k -> new TrieNode());
            }
            cur.phones.add(phone);
        }

        void remove(String name, String phone) {
            TrieNode cur = root;
            for (char ch : name.toLowerCase().toCharArray()) {
                cur = cur.children.get(ch);
                if (cur == null) return;
            }
            cur.phones.remove(phone);
        }

        List<String> startsWith(String prefix) {
            TrieNode cur = root;
            for (char ch : prefix.toLowerCase().toCharArray()) {
                cur = cur.children.get(ch);
                if (cur == null) return new ArrayList<>();
            }
            List<String> result = new ArrayList<>();
            collect(cur, result);
            return result;
        }

        private void collect(TrieNode node, List<String> result) {
            result.addAll(node.phones);
            for (TrieNode child : node.children.values()) {
                collect(child, result);
            }
        }
    }

    // ---------------- Merge Sort ----------------
    static void mergeSort(List<Contact> list, Comparator<Contact> cmp) {
        if (list.size() <= 1) return;
        int mid = list.size() / 2;
        List<Contact> left = new ArrayList<>(list.subList(0, mid));
        List<Contact> right = new ArrayList<>(list.subList(mid, list.size()));
        mergeSort(left, cmp);
        mergeSort(right, cmp);

        int i = 0, j = 0, k = 0;
        while (i < left.size() && j < right.size()) {
            if (cmp.compare(left.get(i), right.get(j)) <= 0) list.set(k++, left.get(i++));
            else list.set(k++, right.get(j++));
        }
        while (i < left.size()) list.set(k++, left.get(i++));
        while (j < right.size()) list.set(k++, right.get(j++));
    }

    // ---------------- Data structures ----------------
    private static final Map<String, Contact> byPhone = new HashMap<>();
    private static final Trie nameTrie = new Trie();
    private static final Deque<Contact> deletedStack = new ArrayDeque<>();
    private static final String FILE_NAME = "contacts.txt";
    private static final Scanner sc = new Scanner(System.in);

    // ---------------- Operations ----------------
    static void addContact() {
        String name = readNonEmpty("Name: ");
        String phone = readPhone("Phone (10 digits): ");
        if (byPhone.containsKey(phone)) {
            System.out.println("This phone number is already saved.");
            return;
        }
        System.out.print("Email (optional): ");
        String email = sc.nextLine().trim();
        System.out.print("Address (optional): ");
        String address = sc.nextLine().trim();

        Contact c = new Contact(name, phone, email, address);
        byPhone.put(phone, c);
        nameTrie.insert(name, phone);
        System.out.println("Contact added.");
    }

    static void searchByPhone() {
        String phone = readNonEmpty("Enter phone: ");
        Contact c = byPhone.get(phone);
        if (c == null) System.out.println("No contact found.");
        else {
            printHeader();
            System.out.println(c);
        }
    }

    static void searchByNamePrefix() {
        String prefix = readNonEmpty("Enter name or starting letters: ");
        List<Contact> found = new ArrayList<>();
        for (String phone : nameTrie.startsWith(prefix)) {
            found.add(byPhone.get(phone));
        }
        if (found.isEmpty()) {
            System.out.println("No contact found.");
            return;
        }
        mergeSort(found, Comparator.comparing(c -> c.name.toLowerCase()));
        printHeader();
        found.forEach(System.out::println);
    }

    static void viewAllSorted() {
        if (byPhone.isEmpty()) {
            System.out.println("No contacts saved yet.");
            return;
        }
        List<Contact> all = new ArrayList<>(byPhone.values());
        mergeSort(all, Comparator.comparing(c -> c.name.toLowerCase()));
        printHeader();
        all.forEach(System.out::println);
        System.out.println("Total: " + all.size());
    }

    static void updateContact() {
        String phone = readNonEmpty("Phone of the contact to update: ");
        Contact c = byPhone.get(phone);
        if (c == null) {
            System.out.println("No contact found.");
            return;
        }
        System.out.println("Press Enter to keep the old value.");
        System.out.print("New name [" + c.name + "]: ");
        String name = sc.nextLine().trim();
        System.out.print("New email [" + c.email + "]: ");
        String email = sc.nextLine().trim();
        System.out.print("New address [" + c.address + "]: ");
        String address = sc.nextLine().trim();

        if (!name.isEmpty()) {
            nameTrie.remove(c.name, c.phone);
            c.name = name;
            nameTrie.insert(c.name, c.phone);
        }
        if (!email.isEmpty()) c.email = email;
        if (!address.isEmpty()) c.address = address;
        System.out.println("Contact updated.");
    }

    static void deleteContact() {
        String phone = readNonEmpty("Phone of the contact to delete: ");
        Contact c = byPhone.remove(phone);
        if (c == null) {
            System.out.println("No contact found.");
            return;
        }
        nameTrie.remove(c.name, c.phone);
        deletedStack.push(c);
        System.out.println("Deleted " + c.name + ". You can undo this from the menu.");
    }

    static void undoDelete() {
        if (deletedStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        Contact c = deletedStack.pop();
        if (byPhone.containsKey(c.phone)) {
            System.out.println("Cannot restore: this phone number is now used by another contact.");
            return;
        }
        byPhone.put(c.phone, c);
        nameTrie.insert(c.name, c.phone);
        System.out.println("Restored " + c.name + ".");
    }

    // ---------------- File handling ----------------
    static void saveToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Contact c : byPhone.values()) {
                out.println(c.name + "|" + c.phone + "|" + c.email + "|" + c.address);
            }
        } catch (IOException e) {
            System.out.println("Could not save contacts: " + e.getMessage());
        }
    }

    static void loadFromFile() {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", -1);
                if (p.length < 4) continue;
                Contact c = new Contact(p[0], p[1], p[2], p[3]);
                byPhone.put(c.phone, c);
                nameTrie.insert(c.name, c.phone);
            }
        } catch (IOException e) {
            System.out.println("Could not load contacts: " + e.getMessage());
        }
    }

    // ---------------- Input helpers ----------------
    static String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("This field cannot be empty.");
        }
    }

    static String readPhone(String prompt) {
        while (true) {
            String s = readNonEmpty(prompt);
            if (s.matches("\\d{10}")) return s;
            System.out.println("Phone number must be exactly 10 digits.");
        }
    }

    static void printHeader() {
        System.out.println(String.format("%-20s %-12s %-25s %s", "NAME", "PHONE", "EMAIL", "ADDRESS"));
        System.out.println("-".repeat(75));
    }

    // ---------------- Main ----------------
    public static void main(String[] args) {
        loadFromFile();
        while (true) {
            System.out.println("\n===== CONTACT MANAGEMENT SYSTEM =====");
            System.out.println("1. Add contact");
            System.out.println("2. Search by phone");
            System.out.println("3. Search by name (prefix)");
            System.out.println("4. View all (sorted by name)");
            System.out.println("5. Update contact");
            System.out.println("6. Delete contact");
            System.out.println("7. Undo last delete");
            System.out.println("0. Save and exit");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": addContact(); break;
                case "2": searchByPhone(); break;
                case "3": searchByNamePrefix(); break;
                case "4": viewAllSorted(); break;
                case "5": updateContact(); break;
                case "6": deleteContact(); break;
                case "7": undoDelete(); break;
                case "0":
                    saveToFile();
                    System.out.println("Contacts saved. Goodbye!");
                    return;
                default: System.out.println("Invalid choice. Enter a number from 0 to 7.");
            }
        }
    }
}