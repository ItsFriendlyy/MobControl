package com.example.mobcontrol;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * الكلاس الرئيسي للإضافة: يدير الإعدادات ومستمعي الأحداث والأوامر
 */
public class MobControlPlugin extends JavaPlugin implements Listener, CommandExecutor {

    // الحدود والقيم الثابتة
    private static final int BASE_RATE = 16;
    private static final int MIN_RATE = 1;
    private static final int MAX_RATE = 32;

    // المتغير البرمجي الحامل للمعدل الحالي
    private int currentRate = BASE_RATE;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        // حفظ وقراءة ملف config.yml
        saveDefaultConfig();
        loadConfiguration();

        // تسجيل مستمع الأحداث والأوامر
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("mobcontrol") != null) {
            getCommand("mobcontrol").setExecutor(this);
        }

        getLogger().info("MobControl has been enabled! Current rate: " + currentRate);
    }

    @Override
    public void onDisable() {
        getLogger().info("MobControl has been disabled.");
    }

    /**
     * قراءة القيمة والتأكد من بقائها ضمن النطاق 1 إلى 32
     */
    private void loadConfiguration() {
        reloadConfig();
        int savedRate = getConfig().getInt("spawn-rate", BASE_RATE);
        this.currentRate = Math.clamp(savedRate, MIN_RATE, MAX_RATE);
    }

    /**
     * حفظ وتحديث القيمة الجديدة في ملف config.yml
     */
    private void setSpawnRate(int newRate) {
        this.currentRate = newRate;
        getConfig().set("spawn-rate", newRate);
        saveConfig();
    }

    /**
     * اعتراض حدث ظهور الوحوش المعادية الطبيعي
     */
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        // التحقق من أن الكائن وحش معادٍ
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        // التدخل فقط مع الظهور الطبيعي في العالم
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        // إذا كان المعدل أقل من 16 (تقليل الظهور بنسبة احتمالية)
        if (currentRate < BASE_RATE) {
            int roll = random.nextInt(BASE_RATE) + 1;
            if (roll > currentRate) {
                event.setCancelled(true);
            }
        } 
        // إذا كان المعدل أكبر من 16 (زيادة الظهور باستدعاء وحوش إضافية)
        else if (currentRate > BASE_RATE) {
            int extraChance = currentRate - BASE_RATE;
            if (random.nextInt(BASE_RATE) < extraChance) {
                event.getLocation().getWorld().spawn(
                    event.getLocation(),
                    event.getEntity().getClass(),
                    CreatureSpawnEvent.SpawnReason.CUSTOM,
                    spawned -> {}
                );
            }
        }
    }

    /**
     * معالجة أوامر الشات
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mobcontrol")) {
            return false;
        }

        // التحقق من صلاحية الإدارة
        if (!sender.hasPermission("mobcontrol.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mobcontrol <set <1-32> | reload>");
            sender.sendMessage(ChatColor.GRAY + "Current spawn rate: " + ChatColor.GREEN + currentRate);
            return true;
        }

        // أمر إعادة التحميل: /mobcontrol reload
        if (args[0].equalsIgnoreCase("reload")) {
            loadConfiguration();
            sender.sendMessage(ChatColor.GREEN + "MobControl configuration reloaded! Current rate: " + currentRate);
            return true;
        }

        // أمر تحديد المعدل: /mobcontrol set <number>
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Please specify a value between 1 and 32.");
                return true;
            }

            try {
                int value = Integer.parseInt(args[1]);

                if (value < MIN_RATE || value > MAX_RATE) {
                    sender.sendMessage(ChatColor.RED + "The rate must be between " + MIN_RATE + " and " + MAX_RATE + ".");
                    return true;
                }

                setSpawnRate(value);
                sender.sendMessage(ChatColor.GREEN + "Spawn rate set to: " + ChatColor.GOLD + value);

            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Please enter a valid whole number.");
            }
            return true;
        }

        return true;
    }
}
