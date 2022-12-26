package ru.vukit.dc.sensors;

import ru.vukit.dc.SensorsFragmentState;

abstract public class DCSensor {

    public final Integer id;
    public final Integer code;
    public final String name;
    public final String settings;
    public Integer valuesLength;
    private final String[] units;
    private final byte format;
    public static SensorsFragmentState sensorsFragmentState;
    private final StringBuilder output = new StringBuilder();

    DCSensor(Integer id, Integer code, String name, String settings, byte format) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.settings = settings;
        this.valuesLength = Integer.parseInt(SensorFactory.parameters.get(code)[1]);
        this.units = new String[this.valuesLength];
        this.format = format;
        for (int i = 0; i < valuesLength; i++) {
            this.units[i] = SensorFactory.parameters.get(code)[i + 3];
        }
    }

    public abstract String getValue(int i);

    public StringBuilder getData() {
        output.setLength(0);
        output.append("{");
        switch (format) {
            case 0: // Не включать имя и единицы измерения датчика
                for (int i = 0; i < valuesLength; i++) {
                    output.append("\"value");
                    output.append(i);
                    output.append("\":");
                    output.append(getValue(i));
                    output.append(",");
                }
                break;
            case 1: // Не включать единицы измерения датчика
                output.append("\"name\":\"");
                output.append(name);
                output.append("\",");
                for (int i = 0; i < valuesLength; i++) {
                    output.append("\"value");
                    output.append(i);
                    output.append("\":");
                    output.append(getValue(i));
                    output.append(",");
                }
                break;
            case 2: // Не включать имя датчика
                for (int i = 0; i < valuesLength; i++) {
                    output.append("\"value");
                    output.append(i);
                    output.append("\":");
                    if (!units[i].isEmpty()) {
                        output.append("\"");
                        output.append(getValue(i));
                        output.append(" ");
                        output.append(units[i]);
                        output.append("\"");
                    } else {
                        output.append(getValue(i));
                    }
                    output.append(",");
                }
                break;
            default:
            case 3: // Включать имя и единицы измерения датчика
                output.append("\"name\":\"");
                output.append(name);
                output.append("\",");
                for (int i = 0; i < valuesLength; i++) {
                    output.append("\"value");
                    output.append(i);
                    output.append("\":");
                    if (!units[i].isEmpty()) {
                        output.append("\"");
                        output.append(getValue(i));
                        output.append(" ");
                        output.append(units[i]);
                        output.append("\"");
                    } else {
                        output.append(getValue(i));
                    }
                    output.append(",");
                }
                break;
        }
        output.setLength(output.length() - 1);
        output.append("}");
        return output;
    }

}
