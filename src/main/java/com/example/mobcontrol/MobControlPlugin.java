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
 * الكلاس الرئيسي للإضافة: يدير الإعدادات، مستمعي الأحداث، والأوامر
 */
public class MobControlPlugin extends JavaPlugin implements Listener, CommandExecutor {

    // الحدود والقيم الثابتة المتفق عليها
    private static final int BASE_RATE = 16;
    private static final int MIN_RATE = 1;
    private static final int MAX_RATE = 32;

    // المتغير البرمجي لتخزين المعدل المطبق ومولد الأرقام العشوائية
    private int currentRate = BASE_RATE;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        // حفظ وقراءة ملف الإعدادات config.yml
        saveDefaultConfig();
        loadConfiguration();

        // تسجيل مراقبة الأحداث ومسؤول الأوامر
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
     * اعتراض حدث ظهور الوحوش وتطبيق الحسابات البرمجية
     */
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        // التأكد من أن الكائن وحش معادٍ (Monster)
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        // التدخل فقط مع الظهور الطبيعي للوحوش في العالم لتجنب السباونرز
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        // الحالة الأولى: إذا كان المعدل أقل من 16 (تقليل الظهور)
        if (currentRate < BASE_RATE) {
            int roll = random.nextInt(BASE_RATE) + 1;
            if (roll > currentRate) {
                // إلغاء ظهور الوحش
                event.setCancelled(true);
            }
        } 
        // الحالة الثانية: إذا كان المعدل أكبر من 16 (زيادة الكثافة)
        else if (currentRate > BASE_RATE) {
            int extraChance = currentRate - BASE_RATE;
            if (random.nextInt(BASE_RATE) < extraChance) {
                // استدعاء وحش إضافي مطابق في نفس الموقع عبر نوع الكائن EntityType بأمان
                event.getLocation().getWorld().spawnEntity(
                    event.getLocation(),
                    event.getEntityType()
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

        // التحقق من صلاحية المشرف
        if (!sender.hasPermission("mobcontrol.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // عند كتابة الأمر مجرداً
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mobcontrol <set <1-32> | reload>");
            sender.sendMessage(ChatColor.GRAY + "Current spawn rate: " + ChatColor.GREEN + currentRate);
            return true;
        }

        // تنفيذ أمر إعادة التحميل: /mobcontrol reload
        if (args[0].equalsIgnoreCase("reload")) {
            loadConfiguration();
            sender.sendMessage(ChatColor.GREEN + "MobControl configuration reloaded! Current rate: " + currentRate);
            return true;
        }

        // تنفيذ أمر تحديد المعدل: /mobcontrol set <number>
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Please specify a number between 1 and 32.");
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
                sender.sendMessage(ChatColor.RED + "Invalid number format. Please enter an integer.");
            }
            return true;
        }

        return true;
    }
}
