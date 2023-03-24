package com.segc;

import com.segc.exception.DuplicateElementException;

import javax.swing.*;
import java.nio.file.Path;
import java.util.*;

/**
 * @author fc54685 Francisco Correia
 * @author fc55955 Alexandre Fonseca
 * @author fc56272 Filipe Egipto
 */
public class WineCatalog {
    public final String wineDataDir;
    private final Map<String, Wine> wines;
    private final DataPersistenceService<Wine> dps;

    public WineCatalog() {
        this.wines = new HashMap<>();
        this.dps = new DataPersistenceService<>();
        this.wineDataDir = Configuration.getInstance().getValue("wineDataDir");
        dps.getObjects(wineDataDir).forEach(wine -> wines.put(wine.getName(), wine));
    }

    public void add(String wineName, ImageIcon label) throws DuplicateElementException {
        if (wines.containsKey(wineName)) {
            throw new DuplicateElementException();
        }
        Wine wine = new Wine(wineName, label);
        wines.put(wineName, wine);
        dps.putObject(wine, Path.of(wineDataDir, wineName));
    }

    public void sell(String wineName, String sellerId, double costPerUnit, int quantity)
            throws DuplicateElementException, NoSuchElementException, IllegalArgumentException {
        Wine wine = Optional.ofNullable(wines.get(wineName)).orElseThrow();
        WineListing wl = new WineListing(sellerId, costPerUnit, quantity);
        if (wine.getListings().putIfAbsent(sellerId, wl) == null) {
            dps.putObject(wine, Path.of(wineDataDir, wine.getName()));
        } else {
            throw new DuplicateElementException();
        }
    }

    public String view(String wineName) throws NoSuchElementException {
        return Optional.ofNullable(wines.get(wineName)).orElseThrow().toString();
    }

    public double buy(String wineName, String sellerId, int quantity)
            throws NoSuchElementException, IllegalArgumentException {
        Wine wine = Optional.ofNullable(wines.get(wineName)).orElseThrow();
        WineListing wl = Optional.ofNullable(wine.getListings().get(sellerId)).orElseThrow();
        wl.removeQuantity(quantity);
        if (wl.getQuantity() == 0) {
            wine.removeListing(sellerId);
        }
        dps.putObject(wine, Path.of(wineDataDir, wine.getName()));
        return wl.getCostPerUnit() * quantity;
    }

    public double getPrice(String wineName, String sellerId, int quantity)
            throws NoSuchElementException, IllegalArgumentException {
        WineListing wl = Optional.ofNullable(wines.get(wineName)).orElseThrow().getListing(sellerId);
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer.");
        }
        return wl.getCostPerUnit() * quantity;

    }

    public void classify(String wineName, int stars) throws NoSuchElementException, IllegalArgumentException {
        Wine wine = wines.get(wineName);
        Optional.ofNullable(wine).orElseThrow().addRating(stars);
        dps.putObject(wine, Path.of(wineDataDir, wine.getName()));
    }
}
