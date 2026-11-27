package org.modernash.renovatio.generated.cobol;

import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.TreeMap;

/**
 * In-memory Cics001CustomersPort for golden-master runs and tests.
 */
public class Cics001CustomersInMemoryAdapter implements Cics001CustomersPort {
  private final TreeMap<String, String> records = new TreeMap<>();

  private String mode = "I-O";

  private String position;

  private String lastRead;

  private long sequence;

  private String startedAt;

  public Cics001CustomersInMemoryAdapter() {
  }

  /**
   * Starts with the given records already in the file.
   */
  public Cics001CustomersInMemoryAdapter(List<String> initialRecords) {
    for (String record : initialRecords) {
      records.put(keyOf(record), record);
    }
  }

  /**
   * Current file content in key (or write) order.
   */
  public List<String> records() {
    return List.copyOf(records.values());
  }

  private String keyOf(String record) {
    return String.format("%019d", sequence++);
  }

  @Override
  public String open(String mode) {
    if (this.mode != null) {
      return "41";
    }
    if ("OUTPUT".equals(mode)) {
      records.clear();
    }
    this.mode = mode;
    this.position = null;
    this.lastRead = null;
    return "00";
  }

  @Override
  public String close() {
    if (mode == null) {
      return "42";
    }
    mode = null;
    return "00";
  }

  @Override
  public Cics001CustomersPort.ReadResult read() {
    if (!"INPUT".equals(mode) && !"I-O".equals(mode)) {
      return new Cics001CustomersPort.ReadResult("47", null);
    }
    startedAt = null;
    String next = position == null ? (records.isEmpty() ? null : records.firstKey()) : records.higherKey(position);
    if (next == null) {
      return new Cics001CustomersPort.ReadResult("10", null);
    }
    position = next;
    lastRead = next;
    return new Cics001CustomersPort.ReadResult("00", records.get(next));
  }

  @Override
  public Cics001CustomersPort.ReadResult read(String key) {
    if (!"INPUT".equals(mode) && !"I-O".equals(mode)) {
      return new Cics001CustomersPort.ReadResult("47", null);
    }
    String record = records.get(key);
    if (record == null) {
      return new Cics001CustomersPort.ReadResult("23", null);
    }
    position = key;
    lastRead = key;
    return new Cics001CustomersPort.ReadResult("00", record);
  }

  @Override
  public String write(String record) {
    if (!"OUTPUT".equals(mode) && !"EXTEND".equals(mode) && !"I-O".equals(mode)) {
      return "48";
    }
    String key = keyOf(record);
    if (records.containsKey(key)) {
      return "22";
    }
    records.put(key, record);
    return "00";
  }

  @Override
  public String rewrite(String record) {
    if (!"I-O".equals(mode)) {
      return "49";
    }
    String key = lastRead;
    if (key == null) {
      return "43";
    }
    if (!records.containsKey(key)) {
      return "23";
    }
    records.put(key, record);
    return "00";
  }

  @Override
  public String delete(String key) {
    if (!"I-O".equals(mode)) {
      return "49";
    }
    String target = key == null ? lastRead : key;
    if (target == null || records.remove(target) == null) {
      return "23";
    }
    return "00";
  }

  @Override
  public String start(String key) {
    if (!"INPUT".equals(mode) && !"I-O".equals(mode)) {
      return "47";
    }
    String first = records.ceilingKey(key);
    if (first == null) {
      return "23";
    }
    position = records.lowerKey(first);
    startedAt = first;
    return "00";
  }

  @Override
  public Cics001CustomersPort.ReadResult readPrevious() {
    String previous = startedAt != null ? startedAt : position == null ? (records.isEmpty() ? null : records.lastKey()) : records.lowerKey(position);
    startedAt = null;
    if (previous == null) {
      return new Cics001CustomersPort.ReadResult("10", null);
    }
    position = previous;
    lastRead = previous;
    return new Cics001CustomersPort.ReadResult("00", records.get(previous));
  }
}
