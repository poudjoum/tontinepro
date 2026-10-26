package com.tontinepro.tontinepro_backend.api.session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Composition des lots d'une session « à lot », sans dépendance Spring.
 *
 * <p>Chaque mise est découpée en lots pleins ; les reliquats sont ensuite regroupés
 * en combinaisons <b>exactes</b> (10 000 + 10 000 + 30 000, 25 000 + 25 000…) : un lot
 * ne dépasse jamais le montant prévu, et on privilégie les lots à peu de membres.
 * Les reliquats qu'aucune combinaison exacte n'absorbe sont rendus comme « restes ».
 */
public final class CompositionLots {

    /** Garde-fou : au-delà, la recherche exacte est trop coûteuse (montants très fins). */
    private static final long MAX_CELLULES = 50_000_000L;

    private CompositionLots() {}

    /** Mise mensuelle d'un membre. */
    public record Mise(UUID membreId, BigDecimal montant) {}

    /** Part d'un membre dans un lot. */
    public record Part(UUID membreId, BigDecimal montant) {}

    /** Résultat : lots exacts formés et reliquats non placés. */
    public record Proposition(List<List<Part>> lots, List<Part> restes) {
        public boolean complete() { return restes.isEmpty(); }
    }

    /** Propose une composition. {@code random} départage les combinaisons équivalentes. */
    public static Proposition proposer(List<Mise> mises, BigDecimal montantLot, Random random) {
        long lot = centimes(montantLot);
        List<List<Part>> lots = new ArrayList<>();
        List<Part> reliquats = new ArrayList<>();

        for (Mise m : mises) {
            long mise = centimes(m.montant());
            for (long i = 0; i < mise / lot; i++) {
                lots.add(List.of(new Part(m.membreId(), montantLot)));
            }
            long reste = mise % lot;
            if (reste > 0) {
                reliquats.add(new Part(m.membreId(), versMontant(reste)));
            }
        }

        // Former, tant que possible, la combinaison exacte la plus courte
        while (!reliquats.isEmpty()) {
            Collections.shuffle(reliquats, random);
            List<Integer> indices = plusPetiteCombinaisonExacte(reliquats, lot);
            if (indices == null) break;
            List<Part> combinaison = new ArrayList<>();
            for (int i : indices) combinaison.add(reliquats.get(i));
            indices.stream().sorted(Collections.reverseOrder()).forEach(i -> reliquats.remove((int) i));
            lots.add(List.copyOf(combinaison));
        }

        return new Proposition(lots, List.copyOf(reliquats));
    }

    /**
     * Vérifie une composition saisie par le bureau : chaque lot vaut exactement le
     * montant du lot, et chaque membre y place exactement sa mise. Les parts d'un même
     * membre dans un même lot sont fusionnées.
     *
     * @return la composition normalisée
     * @throws IllegalArgumentException si la composition est incohérente
     */
    public static List<List<Part>> valider(List<List<Part>> composition, List<Mise> mises,
                                           BigDecimal montantLot) {
        if (composition == null || composition.isEmpty()) {
            throw new IllegalArgumentException("La composition ne contient aucun lot");
        }
        long lot = centimes(montantLot);
        Map<UUID, Long> attendu = new HashMap<>();
        for (Mise m : mises) attendu.merge(m.membreId(), centimes(m.montant()), Long::sum);

        Map<UUID, Long> place = new HashMap<>();
        List<List<Part>> normalisee = new ArrayList<>();
        int numero = 1;
        for (List<Part> parts : composition) {
            Map<UUID, Long> fusion = new LinkedHashMap<>();
            for (Part p : parts == null ? List.<Part>of() : parts) {
                if (p.membreId() == null || !attendu.containsKey(p.membreId())) {
                    throw new IllegalArgumentException("Lot " + numero + " : membre sans mise dans cette session");
                }
                long montant = p.montant() == null ? 0 : centimes(p.montant());
                if (montant <= 0) {
                    throw new IllegalArgumentException("Lot " + numero + " : une part doit être strictement positive");
                }
                fusion.merge(p.membreId(), montant, Long::sum);
            }
            long total = fusion.values().stream().mapToLong(Long::longValue).sum();
            if (total != lot) {
                throw new IllegalArgumentException("Lot " + numero + " : total " + versMontant(total).toPlainString()
                        + " au lieu de " + montantLot.toPlainString());
            }
            List<Part> lotNormalise = new ArrayList<>();
            fusion.forEach((membreId, montant) -> {
                place.merge(membreId, montant, Long::sum);
                lotNormalise.add(new Part(membreId, versMontant(montant)));
            });
            normalisee.add(lotNormalise);
            numero++;
        }

        for (Map.Entry<UUID, Long> e : attendu.entrySet()) {
            long reparti = place.getOrDefault(e.getKey(), 0L);
            if (reparti != e.getValue()) {
                throw new IllegalArgumentException("Un membre a " + versMontant(reparti).toPlainString()
                        + " répartis dans les lots pour une mise de " + versMontant(e.getValue()).toPlainString());
            }
        }
        return normalisee;
    }

    /**
     * Sous-ensemble de cardinalité minimale dont la somme vaut exactement {@code cible}
     * (sac à dos 0/1 en unités de PGCD). {@code null} si aucun.
     */
    private static List<Integer> plusPetiteCombinaisonExacte(List<Part> items, long cible) {
        int n = items.size();
        long[] montants = new long[n];
        long pgcd = cible;
        for (int i = 0; i < n; i++) {
            montants[i] = centimes(items.get(i).montant());
            pgcd = pgcd(pgcd, montants[i]);
        }
        long unitesCible = cible / pgcd;
        if (unitesCible * n > MAX_CELLULES) return null;
        int t = (int) unitesCible;

        int infini = Integer.MAX_VALUE;
        int[] minimum = new int[t + 1];
        java.util.Arrays.fill(minimum, infini);
        minimum[0] = 0;
        boolean[][] pris = new boolean[n][t + 1];

        for (int i = 0; i < n; i++) {
            int w = (int) (montants[i] / pgcd);
            for (int s = t; s >= w; s--) {
                if (minimum[s - w] != infini && minimum[s - w] + 1 < minimum[s]) {
                    minimum[s] = minimum[s - w] + 1;
                    pris[i][s] = true;
                }
            }
        }
        if (minimum[t] == infini) return null;

        List<Integer> indices = new ArrayList<>();
        int s = t;
        for (int i = n - 1; i >= 0 && s > 0; i--) {
            if (pris[i][s]) {
                indices.add(i);
                s -= (int) (montants[i] / pgcd);
            }
        }
        return indices;
    }

    private static long pgcd(long a, long b) {
        while (b != 0) { long r = a % b; a = b; b = r; }
        return a;
    }

    private static long centimes(BigDecimal montant) {
        return montant.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }

    private static BigDecimal versMontant(long centimes) {
        return BigDecimal.valueOf(centimes, 2);
    }
}
