package com.segc;

import com.segc.exception.DuplicateElementException;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class WineCatalog {
    private final Map<String, Wine> wines;
    private final DataPersistenceService<Wine> dps;
    public final String wineDataDir;

    public WineCatalog() {
        this.wines = new HashMap<>();
        this.dps = new DataPersistenceService<>();
        this.wineDataDir = Configuration.getInstance().getValue("wineDataDir");
        dps.getObjects(wineDataDir).forEach(wine -> wines.put(wine.getName(), wine));
    }

    public void addListing(String wineName, String sellerId, double costPerUnit, int quantity) {
        Wine wine = wines.get(wineName);
        if (wine == null) {
            throw new NoSuchElementException();
        }
        WineListing wineListing = new WineListing(sellerId, costPerUnit, quantity);
        if (wine.getListings().putIfAbsent(sellerId, wineListing) == null) {
            dps.putObject(wine, Path.of(wineDataDir, wine.getName()));
        } else {
            throw new DuplicateElementException();
        }
    }

    public void addWine(String wineName, String labelPath) {
        Wine wine = new Wine(wineName, labelPath);
        if (wines.putIfAbsent(wineName, wine) != null) {
            throw new DuplicateElementException();
        }
    }

}
