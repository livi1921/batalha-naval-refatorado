import java.util.*;

/*
 * Main.java
 * Batalha Naval monolitica para refatoracao.
 *
 * Intencionalmente:
 * - Tudo em uma classe, estado global espalhado.
 * - Log, UI, regras, validacoes e IA misturados.
 * - Duplicacao de logica em varios pontos.
 * - Estruturas primitivas e arrays paralelos.
 *
 * Regras:
 * - Tabuleiro 10x10 (A-J, 1-10).
 * - Frota classica: 5, 4, 3, 3, 2.
 * - Sem diagonal.
 * - Tiro: agua, acerto, afundou.
 * - Vence quem afundar todos os navios do adversario.
 */
public class Main {
    // Codigos usados pelo motor para representar o resultado de um tiro.
    static final int AGUA = 0;
    static final int ACERTO = 1;
    static final int AFUNDOU = 2;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== BATALHA NAVAL (MONOLITO) ===");
        System.out.print("Seed (vazio para aleatorio): ");
        String seedStr = sc.nextLine().trim();
        Random rng;
        if (seedStr.isEmpty()) rng = new Random();
        else {
            long seed;
            try { seed = Long.parseLong(seedStr); }
            catch (Exception e) { seed = seedStr.hashCode(); }
            rng = new Random(seed);
        }

        final int tamanhoTabuleiro = 10;
        final int[] tamanhosNavios = new int[] {5, 4, 3, 3, 2};
        final String[] nomesNavios = new String[] {"Porta-avioes", "Encouracado", "Cruzador", "Submarino", "Destroyer"};

        ValidadorDeFrotas validadorDeFrotas = new ValidadorDeFrotas();

        if (!validadorDeFrotas.validarTamanhos(tamanhosNavios)) {
            System.out.println("Configuracao de frota invalida.");
            sc.close();
            return;
        }
        
        // Tabuleiros
        // ownShips: mostra navios do jogador. '.' vazio, 'S' navio, 'X' acerto no proprio, 'o' agua no proprio (raro).
        // ownShots: mapa de tiros do jogador no inimigo. '.' desconhecido, 'X' acerto, 'o' agua.
        // cpuShips: navios da CPU. '.' vazio, 'S' navio, 'X' acerto.
        // cpuShots: tiros da CPU no jogador. '.' desconhecido, 'X' acerto, 'o' agua.
        char[][] ownShips = new char[tamanhoTabuleiro][tamanhoTabuleiro];
        char[][] ownShots = new char[tamanhoTabuleiro][tamanhoTabuleiro];
        char[][] cpuShips = new char[tamanhoTabuleiro][tamanhoTabuleiro];
        char[][] cpuShots = new char[tamanhoTabuleiro][tamanhoTabuleiro];

        fill(ownShips, '.');
        fill(ownShots, '.');
        fill(cpuShips, '.');
        fill(cpuShots, '.');

        // Mapas de navio por celula, -1 vazio, caso contrario id 0..4
        int[][] ownShipId = new int[tamanhoTabuleiro][tamanhoTabuleiro];
        int[][] cpuShipId = new int[tamanhoTabuleiro][tamanhoTabuleiro];
        fillInt(ownShipId, -1);
        fillInt(cpuShipId, -1);

        // HP de cada navio
        int[] ownHp = Arrays.copyOf(tamanhosNavios, tamanhosNavios.length);
        int[] cpuHp = Arrays.copyOf(tamanhosNavios, tamanhosNavios.length);
        
        if (!frotaEstaCompleta(ownHp) || !frotaEstaCompleta(cpuHp)) {
            System.out.println("Erro ao inicializar a vida dos navios.");
            sc.close();
            return;
        }

        // Log
        String[] log = new String[5000];
        int logSize = 0;

        // Posicionamento do jogador
        System.out.println();
        System.out.println("Posicionamento da sua frota.");
        System.out.println("Coordenadas: A-J e 1-10. Ex: A1, J10.");
        System.out.print("Deseja posicionar manualmente? (s/N): ");
        String manual = sc.nextLine().trim().toLowerCase(Locale.ROOT);

        if (manual.equals("s") || manual.equals("sim")) {
            for (int sid = 0; sid < tamanhosNavios.length; sid++) {
                boolean placed = false;
                while (!placed) {
                    System.out.println();
                    printSingleBoard("SEU TABULEIRO", ownShips, true);
                    System.out.println("Posicione: " + nomesNavios[sid] + " (tamanho " + tamanhosNavios[sid] + ")");
                    System.out.print("Informe coordenada inicial (ex A1): ");
                    String c = sc.nextLine().trim();
                    Coordenada coordenadaInicial = parseCoordenada(c, tamanhoTabuleiro);
                    if (coordenadaInicial == null) {
                        System.out.println("Coordenada invalida.");
                        continue;
                    }
                    System.out.print("Direcao (H para horizontal, V para vertical): ");
                    String dir = sc.nextLine().trim().toUpperCase(Locale.ROOT);
                    boolean horiz = dir.equals("H");
                    boolean vert = dir.equals("V");
                    if (!horiz && !vert) {
                        System.out.println("Direcao invalida.");
                        continue;
                    }

                    int x = coordenadaInicial.getX();
                    int y = coordenadaInicial.getY();
                    if (!podePosicionarNavio(ownShips, x, y, tamanhosNavios[sid], horiz)) {
                        System.out.println("Posicao invalida: o navio nao cabe, colide ou encosta em outro navio.");
                        continue;
                    }

                    placeShip(ownShips, ownShipId, x, y, tamanhosNavios[sid], horiz, sid);
                    log[logSize++] = "Jogador posicionou " + nomesNavios[sid] + " em " + formatarCoordenada(x, y);
                    placed = true;
                }
            }
        } else {
            // Auto posicionamento jogador
            for (int sid = 0; sid < tamanhosNavios.length; sid++) {
                boolean ok = false;
                int tries = 0;
                while (!ok && tries < 2000) {
                    tries++;
                    boolean horiz = rng.nextInt(2) == 0;
                    int x = rng.nextInt(tamanhoTabuleiro);
                    int y = rng.nextInt(tamanhoTabuleiro);
                    if (podePosicionarNavio(ownShips, x, y, tamanhosNavios[sid], horiz)) {
                        placeShip(ownShips, ownShipId, x, y, tamanhosNavios[sid], horiz, sid);
                        log[logSize++] = "Jogador auto posicionou " + nomesNavios[sid];
                        ok = true;
                    }
                }
                if (!ok) {
                    System.out.println("Falha ao posicionar automaticamente. Algo ficou errado.");
                    sc.close();
                    return;
                }
            }
            System.out.println("Frota posicionada automaticamente.");
        }

        // Posicionamento CPU
        for (int sid = 0; sid < tamanhosNavios.length; sid++) {
            boolean ok = false;
            int tries = 0;
            while (!ok && tries < 5000) {
                tries++;
                boolean horiz = rng.nextInt(2) == 0;
                int x = rng.nextInt(tamanhoTabuleiro);
                int y = rng.nextInt(tamanhoTabuleiro);
                if (podePosicionarNavio(cpuShips, x, y, tamanhosNavios[sid], horiz)) {
                    placeShip(cpuShips, cpuShipId, x, y, tamanhosNavios[sid], horiz, sid);
                    ok = true;
                }
            }
            if (!ok) {
                System.out.println("Falha ao posicionar CPU.");
                sc.close();
                return;
            }
        }

        // Loop do jogo //
        boolean gameOver = false;
        boolean playerTurn = true;

        // IA simples com "alvo"
        // Quando acerta, guarda uma lista de candidatos ao redor para tentar depois.
        ArrayDeque<int[]> cpuTargets = new ArrayDeque<>();
        boolean[][] cpuTried = new boolean[tamanhoTabuleiro][tamanhoTabuleiro];

        while (!gameOver) {
            System.out.println();
            printTwoBoards(ownShips, ownShots);

            int ownAlive = contarNaviosVivos(ownHp);
            int cpuAlive = contarNaviosVivos(cpuHp);

            System.out.println("Navios restantes, voce: " + ownAlive + " | CPU: " + cpuAlive);

            if (frotaFoiAfundada(cpuHp)) {
                System.out.println("VITORIA. Voce afundou toda a frota inimiga.");
                log[logSize++] = "Fim: vitoria do jogador";
                break;
            }
            if (frotaFoiAfundada(ownHp)) {
                System.out.println("DERROTA. Sua frota foi afundada.");
                log[logSize++] = "Fim: vitoria da CPU";
                break;
            }

            if (playerTurn) {
                System.out.println();
                System.out.println("Seu turno.");
                System.out.println("Acoes: 1) Atirar 2) Ver log (ultimos 10) 3) Mostrar seu tabuleiro");
                System.out.print("> ");
                String opt = sc.nextLine().trim();

                if ("2".equals(opt)) {
                    printLogTail(log, logSize, 10);
                    continue;
                }
                if ("3".equals(opt)) {
                    printSingleBoard("SEU TABULEIRO", ownShips, true);
                    continue;
                }

                // Tiro do jogador
                String shot;
                while (true) {
                    System.out.print("Coordenada para atirar (ex B7): ");
                    shot = sc.nextLine().trim();
                    Coordenada coordenada = parseCoordenada(shot, tamanhoTabuleiro);
                    if (coordenada == null) {
                        System.out.println("Invalida.");
                        continue;
                    }

                int x = coordenada.getX();
                int y = coordenada.getY();

                    if (ownShots[y][x] != '.') {
                        System.out.println("Voce ja atirou ai.");
                        continue;
                    }
                    // Aplica tiro na CPU
                    int result = applyShot(cpuShips, cpuShipId, cpuHp, x, y);

                    if (result == AGUA) {
                        ownShots[y][x] = 'o';
                        System.out.println("AGUA.");
                        log[logSize++] = "Jogador errou em " + formatarCoordenada(x, y);
                    } 
                    else {
                        ownShots[y][x] = 'X';

                        int sid = cpuShipId[y][x];

                        if (result == AFUNDOU) {
                            System.out.println("AFUNDOU um navio inimigo: " + nomesNavios[sid]);
                            log[logSize++] = "Jogador afundou " + nomesNavios[sid];
                        } 
                        else {
                            System.out.println("ACERTO.");
                            log[logSize++] = "Jogador acertou em " + formatarCoordenada(x, y);
                        }
                    }

                    playerTurn = false;
                    break;
                }

            } else {
                // Turno CPU
                System.out.println();
                System.out.println("Turno da CPU.");

                // Escolha de alvo
                int tx = -1, ty = -1;

                // Se tem alvos pendentes, tenta primeiro
                while (!cpuTargets.isEmpty()) {
                    int[] t = cpuTargets.removeFirst();
                    int x = t[0], y = t[1];
                    if (x < 0 || x >= tamanhoTabuleiro || y < 0 || y >= tamanhoTabuleiro) continue;
                    if (cpuTried[y][x]) continue;
                    tx = x; ty = y;
                    break;
                }

                // Senao, aleatorio, com leve preferencia por padrao tipo x+y par
                if (tx == -1) {
                    int tries = 0;
                    while (tries < 5000) {
                        tries++;
                        int x = rng.nextInt(tamanhoTabuleiro);
                        int y = rng.nextInt(tamanhoTabuleiro);
                        if (cpuTried[y][x]) continue;
                        // Preferencia simples, mas nao garante
                        if ((x + y) % 2 == 0 || rng.nextInt(100) < 25) {
                            tx = x; ty = y;
                            break;
                        }
                    }
                    if (tx == -1) {
                        // fallback
                        outer:
                        for (int y = 0; y < tamanhoTabuleiro; y++) {
                            for (int x = 0; x < tamanhoTabuleiro; x++) {
                                if (!cpuTried[y][x]) { tx = x; ty = y; break outer; }
                            }
                        }
                    }
                }

                cpuTried[ty][tx] = true;

                // Aplica tiro no jogador
                int result = applyShot(ownShips, ownShipId, ownHp, tx, ty);

                if (result == AGUA) {
                    cpuShots[ty][tx] = 'o';
                    System.out.println("CPU errou em " + formatarCoordenada(tx, ty));
                    log[logSize++] = "CPU errou em " + formatarCoordenada(tx, ty);
                } 
                else {
                    cpuShots[ty][tx] = 'X';

                    int sid = ownShipId[ty][tx];

                    System.out.println("CPU acertou em " + formatarCoordenada(tx, ty));
                    log[logSize++] = "CPU acertou em " + formatarCoordenada(tx, ty);

                    cpuTargets.addLast(new int[]{tx + 1, ty});
                    cpuTargets.addLast(new int[]{tx - 1, ty});
                    cpuTargets.addLast(new int[]{tx, ty + 1});
                    cpuTargets.addLast(new int[]{tx, ty - 1});

                    if (result == AFUNDOU) {
                    System.out.println("CPU AFUNDOU seu navio: " + nomesNavios[sid]);
                    log[logSize++] = "CPU afundou " + nomesNavios[sid];

                    if (rng.nextInt(100) < 60) cpuTargets.clear();
                    }
                }

                playerTurn = true;
            }
        }

        System.out.println();
        System.out.print("Mostrar log completo? (s/N): ");
        String show = sc.nextLine().trim().toLowerCase(Locale.ROOT);
        if (show.equals("s") || show.equals("sim")) {
            for (int i = 0; i < logSize && i < log.length; i++) {
                System.out.println((i + 1) + ") " + log[i]);
            }
        }

        sc.close();
    }

    // ===== Motor do jogo: regras sem leitura de teclado =====

    static void fill(char[][] b, char c) {
        for (int y = 0; y < b.length; y++) {
            for (int x = 0; x < b[y].length; x++) b[y][x] = c;
        }
    }

    static void fillInt(int[][] b, int v) {
        for (int y = 0; y < b.length; y++) {
            for (int x = 0; x < b[y].length; x++) b[y][x] = v;
        }
    }

    // // Converte uma coordenada digitada pelo usuario, como "A1" ou "J10", em posicao x,y do tabuleiro.
    // Retorna null quando a coordenada for invalida.
    static int[] parseCoord(String s, int tamanhoTabuleiro) {
        if (s == null) return null;
        String textoNormalizado = s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (textoNormalizado.length() < 2 || textoNormalizado.length() > 3) return null;

        char col = textoNormalizado.charAt(0);
        int x = col - 'A';

        if (x < 0 || x >= tamanhoTabuleiro) return null;

        String textoLinha = textoNormalizado.substring(1);
        int linha;
        try { 
            linha = Integer.parseInt(textoLinha); 
        }
        catch (Exception e) { 
            return null; 
        }
        if (linha < 1 || linha > tamanhoTabuleiro) return null;
        int y = linha - 1;

        return new int[]{x, y};
    }

    static Coordenada parseCoordenada(String texto, int tamanhoTabuleiro) {
        int[] xy = parseCoord(texto, tamanhoTabuleiro);

        if (xy == null) {
            return null;
        }

        Coordenada coordenada = new Coordenada(xy[0], xy[1]);

        if (!coordenada.estaDentroDoTabuleiro(tamanhoTabuleiro)) {
            return null;
        }

        return coordenada;
    }

    static String formatarCoordenada(int x, int y) {
        char coluna = (char)('A' + x);
        int linha = y + 1;
        return "" + coluna + linha;
    }

    static boolean validarTamanhosDaFrota(int[] tamanhosNavios) {
        if (tamanhosNavios.length != 5) {
            return false;
        }

        int[] esperados = new int[] {2, 3, 3, 4, 5};
        int[] recebidos = Arrays.copyOf(tamanhosNavios, tamanhosNavios.length);

        Arrays.sort(recebidos);

        for (int i = 0; i < esperados.length; i++) {
            if (recebidos[i] != esperados[i]) {
                return false;
            }
        }

        return true;
    }

    static boolean frotaEstaCompleta(int[] vidasDosNavios) {
        for (int vida : vidasDosNavios) {
            if (vida <= 0) {
                return false;
            }
        }

        return true;
    }

    static boolean frotaFoiAfundada(int[] vidasDosNavios) {
        for (int vida : vidasDosNavios) {
            if (vida > 0) {
                return false;
            }
        }

        return true;
    }

    static boolean podePosicionarNavio(char[][] board, int x, int y, int len, boolean horiz) {
        if (x < 0 || x >= board.length || y < 0 || y >= board.length) return false;
        if (horiz) {
            if (x + len > board.length) return false;
            for (int i = 0; i < len; i++) {
                if (board[y][x + i] != '.') return false;
                if (temNavioEncostando(board, x, y + i)) return false;
            }
        } 
        else {
             if (y + len > board.length) return false;
            for (int i = 0; i < len; i++) {
                if (board[y + i][x] != '.') return false;
                if (temNavioEncostando(board, x + i, y)) return false;
            }
        }
        return true;
    }
    // Verifica se existe algum navio encostando na posicao informada.
    // Esta regra representa a validacao ORTHO_DIAG: nao pode encostar pelos lados nem pelas diagonais.
    static boolean temNavioEncostando(char[][] board, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx < 0 || nx >= board.length || ny < 0 || ny >= board.length) {
                    continue;
                }

                if (board[ny][nx] == 'S') {
                    return true;
                }
            }
        }

        return false;
    }

    static void placeShip(char[][] board, int[][] shipId, int x, int y, int len, boolean horiz, int sid) {
        if (horiz) {
            for (int i = 0; i < len; i++) {
                board[y][x + i] = 'S';
                shipId[y][x + i] = sid;
            }
        } else {
            for (int i = 0; i < len; i++) {
                board[y + i][x] = 'S';
                shipId[y + i][x] = sid;
            }
        }
    }

    static int applyShot(char[][] ships, int[][] shipId, int[] hp, int x, int y) {
        if (x < 0 || x >= ships.length || y < 0 || y >= ships.length) {
            return AGUA;
        }

        if (ships[y][x] != 'S') {
            return AGUA;
        }

        ships[y][x] = 'X';

        int sid = shipId[y][x];
        if (sid >= 0) {
            hp[sid]--;
            if (hp[sid] == 0) {
                return AFUNDOU;
            }
        }

        return ACERTO;
    }

    static int contarNaviosVivos(int[] hp) {
        int c = 0;
        for (int v : hp) if (v > 0) c++;
        return c;
    }

    static void printTwoBoards(char[][] ownShips, char[][] ownShots) {
        // Esquerda: seu tabuleiro, com navios
        // Direita: seus tiros no inimigo
        System.out.println(String.format("%-30s | %s", "SEU TABULEIRO", "TIROS NO INIMIGO"));
        System.out.println("    A B C D E F G H I J     |     A B C D E F G H I J");

        for (int y = 0; y < 10; y++) {
            String leftRow = String.format("%2d  ", (y + 1));
            String rightRow = String.format("%2d  ", (y + 1));

            for (int x = 0; x < 10; x++) {
                char c = ownShips[y][x];
                // Mostra navio como 'S', acerto 'X', vazio '.'
                leftRow += c + " ";
            }

            for (int x = 0; x < 10; x++) {
                char c = ownShots[y][x];
                rightRow += c + " ";
            }

            System.out.println(leftRow + "  |  " + rightRow);
        }

        System.out.println("Legenda: S navio, X acerto, o agua, . desconhecido ou vazio");
    }

    static void printSingleBoard(String title, char[][] b, boolean showShips) {
        System.out.println(title);
        System.out.println("    A B C D E F G H I J");
        for (int y = 0; y < 10; y++) {
            String row = String.format("%2d  ", (y + 1));
            for (int x = 0; x < 10; x++) {
                char c = b[y][x];
                if (!showShips && c == 'S') c = '.';
                row += c + " ";
            }
            System.out.println(row);
        }
    }

    static void printLogTail(String[] log, int logSize, int n) {
        System.out.println("Ultimos eventos:");
        int start = Math.max(0, logSize - n);
        for (int i = start; i < logSize && i < log.length; i++) {
            System.out.println((i + 1) + ") " + log[i]);
        }
    }
}
