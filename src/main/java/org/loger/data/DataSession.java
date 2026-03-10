package org.loger.data;

import org.loger.Main;
import org.loger.config.Config;
import org.loger.config.Label;
import org.loger.util.AimProcessor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataSession {
    private static final int COMBAT_TIMEOUT = 40;
    private final AimProcessor aimProcessor;
    private final String comment;
    private final Label label;
    private final ReentrantReadWriteLock lock;
    private final String playerName;
    private final Queue<TickData> recordedTicks;
    private final Instant startTime;
    private int ticksSinceAttack;
    private final UUID uuid;

    public DataSession(UUID uuid, String playerName, Label label, String comment) {
        this(uuid, playerName, label, comment, new AimProcessor());
    }

    public DataSession(UUID uuid, String playerName, Label label, String comment, AimProcessor aimProcessor) {
        this.lock = new ReentrantReadWriteLock();
        this.uuid = uuid;
        this.playerName = playerName;
        this.label = label;
        this.comment = comment;
        this.recordedTicks = new ConcurrentLinkedQueue();
        this.startTime = Instant.now();
        this.aimProcessor = aimProcessor;
        this.ticksSinceAttack = 40;
    }

    public void processTick(float yaw, float pitch) {
        this.lock.writeLock().lock();
        try {
            TickData tickData = this.aimProcessor.process(yaw, pitch);
            this.recordedTicks.add(tickData);
            this.ticksSinceAttack++;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void onAttack() {
        this.lock.writeLock().lock();
        try {
            this.ticksSinceAttack = 0;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public int getTickCount() {
        this.lock.readLock().lock();
        try {
            return this.recordedTicks.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public boolean isInCombat() {
        this.lock.readLock().lock();
        try {
            return this.ticksSinceAttack < 40;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public Label getLabel() {
        return this.label;
    }

    public String getComment() {
        return this.comment;
    }

    public Instant getStartTime() {
        return this.startTime;
    }

    public int getTicksSinceAttack() {
        this.lock.readLock().lock();
        try {
            return this.ticksSinceAttack;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public String generateFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(this.startTime.toEpochMilli()));
        String statusForFilename = this.label.name();
        if (this.comment != null && !this.comment.isEmpty()) {
            String sanitized = this.comment.replace(' ', '#').replaceAll("[/\\\\?%*:|\"<>']", "-");
            statusForFilename = statusForFilename + "_" + sanitized;
        }
        String sanitized2 = this.playerName;
        return String.format("%s_%s_%s.csv", statusForFilename, sanitized2, timestamp);
    }

    public String generateCsvContent() {
        this.lock.readLock().lock();
        try {
            if (this.recordedTicks.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(TickData.getHeader()).append("\n");
            String cheatingStatus = Config.DEFAULT_AUTOSTART_LABEL;
            if (this.label == Label.CHEAT) {
                cheatingStatus = "CHEAT";
            } else if (this.label == Label.LEGIT) {
                cheatingStatus = "LEGIT";
            }
            List<TickData> ticks = new ArrayList<>(this.recordedTicks);
            for (TickData tick : ticks) {
                sb.append(tick.toCsv(cheatingStatus)).append("\n");
            }
            return sb.toString();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void saveAndClose(Main plugin) throws IOException {
        saveAndClose(plugin, null);
    }

    public void saveAndClose(Main plugin, String sessionFolder) throws IOException {
        File dataFolder;
        String csvContent = generateCsvContent();
        if (csvContent.isEmpty()) {
            return;
        }
        String outDir = plugin.getConfig().getString("outputDirectory", "data");
        File baseDir = new File(plugin.getDataFolder(), outDir);
        if (sessionFolder != null && !sessionFolder.isEmpty()) {
            dataFolder = new File(baseDir, sessionFolder);
        } else {
            dataFolder = baseDir;
        }
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File outputFile = new File(dataFolder, generateFileName());
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        try {
            writer.write(csvContent);
            writer.close();
            plugin.getLogger().info("Saved " + this.recordedTicks.size() + " ticks to " + outputFile.getPath());
        } catch (Throwable th) {
            try {
                writer.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }
}
