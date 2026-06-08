package tn.neuron.ardhi.utils.gestionemployeutils;

import tn.neuron.ardhi.models.gestionemployemodel.Employe;
import tn.neuron.ardhi.models.gestionemployemodel.Tache;

import java.util.HashMap;
import java.util.Map;

public class CacheManager {

    private static CacheManager instance; //Une seule instance pour toute l'app
    private Map<Integer, Employe> employeCache;
    private Map<Integer, Tache> tacheCache;

    private CacheManager() {
        employeCache = new HashMap<>();
        tacheCache = new HashMap<>();
    }

    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    public void putEmploye(Integer id, Employe employe) {
        if (id != null && employe != null) {
            employeCache.put(id, employe);
        }
    }

    public Employe getEmploye(Integer id) {
        return id != null ? employeCache.get(id) : null;
    }

    public void putTache(Integer id, Tache tache) {
        if (id != null && tache != null) {
            tacheCache.put(id, tache);
        }
    }

    public Tache getTache(Integer id) {
        return id != null ? tacheCache.get(id) : null;
    }

    public void clearEmployeCache() {
        employeCache.clear();
    }

    public void clearTacheCache() {
        tacheCache.clear();
    }

    public void clearAll() {
        employeCache.clear();
        tacheCache.clear();
    }
}