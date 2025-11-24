import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class DiningPhilosophers {

    // Configuração lida do arquivo
    static class Config {
        int filosofos;
        int duracaoSeg;
        int thinkMinMs, thinkMaxMs;
        int eatMinMs, eatMaxMs;
        String variacao;

        static Config fromFile(String path) throws IOException {
            Config c = new Config();
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] parts = line.split("=");
                    if (parts.length != 2) continue;

                    String key = parts[0].trim().toLowerCase();
                    String value = parts[1].trim();

                    switch (key) {
                        case "filosofos":
                            c.filosofos = Integer.parseInt(value);
                            break;
                        case "duracao_seg":
                            c.duracaoSeg = Integer.parseInt(value);
                            break;
                        case "think_ms": {
                            String[] range = value.split("-");
                            c.thinkMinMs = Integer.parseInt(range[0]);
                            c.thinkMaxMs = Integer.parseInt(range[1]);
                            break;
                        }
                        case "eat_ms": {
                            String[] range = value.split("-");
                            c.eatMinMs = Integer.parseInt(range[0]);
                            c.eatMaxMs = Integer.parseInt(range[1]);
                            break;
                        }
                        case "variacao":
                            c.variacao = value.toLowerCase();
                            break;
                    }
                }
            }
            return c;
        }
    }

    // Flag simples para sinalizar parada
    static class StopFlag {
        private volatile boolean stop = false;

        public void requestStop() {
            this.stop = true;
        }

        public boolean shouldStop() {
            return stop;
        }
    }

    // Filósofo (Thread)
    static class Philosopher extends Thread {
        private final int id;
        private final Semaphore[] forks;
        private final Config config;
        private final Random random = new Random();

        // Estatísticas compartilhadas (1 posição por filósofo)
        private final int[] refeicoes;
        private final long[] esperaTotalMs;

        private final int n;
        private final boolean simetria;

        private final StopFlag stopFlag;

        public Philosopher(int id,
                           Semaphore[] forks,
                           Config config,
                           int[] refeicoes,
                           long[] esperaTotalMs,
                           boolean simetria,
                           StopFlag stopFlag) {
            this.id = id;
            this.forks = forks;
            this.config = config;
            this.refeicoes = refeicoes;
            this.esperaTotalMs = esperaTotalMs;
            this.n = config.filosofos;
            this.simetria = simetria;
            this.stopFlag = stopFlag;
        }

        @Override
        public void run() {
            while (!stopFlag.shouldStop()) {
                // Pensando
                dormirRandom(config.thinkMinMs, config.thinkMaxMs);
                if (stopFlag.shouldStop()) break;

                long inicioEspera = System.currentTimeMillis();

                // Índices dos garfos
                int left = id;
                int right = (id + 1) % n;

                int firstFork, secondFork;

                if (simetria) {
                    // Quebra de simetria:
                    // Filósofos 0..n-2 pegam primeiro o garfo da direita;
                    // o último (n-1) pega primeiro o da esquerda.
                    if (id == n - 1) {
                        firstFork = left;
                        secondFork = right;
                    } else {
                        firstFork = right;
                        secondFork = left;
                    }
                } else {
                    // (Espaço para outras variações no futuro)
                    firstFork = left;
                    secondFork = right;
                }

                try {
                    // Tentar adquirir os dois garfos
                    forks[firstFork].acquire();
                    forks[secondFork].acquire();

                    long fimEspera = System.currentTimeMillis();
                    long espera = fimEspera - inicioEspera;

                    // Atualiza estatísticas do filósofo
                    refeicoes[id]++;
                    esperaTotalMs[id] += espera;

                    // Comendo
                    dormirRandom(config.eatMinMs, config.eatMaxMs);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    // Libera garfos
                    forks[firstFork].release();
                    forks[secondFork].release();
                }
            }
        }

        private void dormirRandom(int minMs, int maxMs) {
            int delta = maxMs - minMs;
            int sleep = (delta > 0) ? minMs + random.nextInt(delta + 1) : minMs;
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java DiningPhilosophers <arquivo_de_entrada>");
            System.out.println("Exemplo: java DiningPhilosophers input_filosofos.txt");
            return;
        }

        String inputPath = args[0];

        try {
            Config config = Config.fromFile(inputPath);

            // ==========================
            // HOSTNAME / COMPROVAÇÃO AWS
            // ==========================
            String hostname;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                hostname = "desconhecido";
            }

            boolean isEc2Aws = hostname.startsWith("ip-172-31-");

            System.out.println("Hostname desta máquina: " + hostname);
            if (isEc2Aws) {
                System.out.println("Identificado padrão de hostname da AWS EC2 (ip-172-31-...).");
            } else {
                System.out.println("Hostname não segue o padrão ip-172-31- (provavelmente fora da EC2).");
            }
            System.out.println();

            if (!"simetria".equals(config.variacao)) {
                System.out.println("Por enquanto, esta implementação suporta apenas variacao=simetria.");
                System.out.println("No seu arquivo está: variacao=" + config.variacao);
                return;
            }

            int n = config.filosofos;
            System.out.println("=== Jantar dos Filósofos ===");
            System.out.println("Filósofos: " + n);
            System.out.println("Duração: " + config.duracaoSeg + " s");
            System.out.println("Think: " + config.thinkMinMs + "-" + config.thinkMaxMs + " ms");
            System.out.println("Eat: " + config.eatMinMs + "-" + config.eatMaxMs + " ms");
            System.out.println("Variação: " + config.variacao);
            System.out.println();

            // Criar garfos (semáforos)
            Semaphore[] forks = new Semaphore[n];
            for (int i = 0; i < n; i++) {
                forks[i] = new Semaphore(1);
            }

            // Estatísticas
            int[] refeicoes = new int[n];
            long[] esperaTotalMs = new long[n];

            StopFlag stopFlag = new StopFlag();

            // Criar e iniciar filósofos
            Philosopher[] philos = new Philosopher[n];
            for (int i = 0; i < n; i++) {
                philos[i] = new Philosopher(
                        i,
                        forks,
                        config,
                        refeicoes,
                        esperaTotalMs,
                        true,      // simetria = true
                        stopFlag
                );
                philos[i].start();
            }

            // Rodar pela duração especificada
            Thread.sleep(config.duracaoSeg * 1000L);

            // Solicita parada
            stopFlag.requestStop();

            // Espera todas as threads terminarem
            for (int i = 0; i < n; i++) {
                philos[i].join();
            }

            // Imprime resultados
            System.out.println("=== Resultados ===");
            for (int i = 0; i < n; i++) {
                int r = refeicoes[i];
                long esperaTotal = esperaTotalMs[i];
                double mediaEspera = (r > 0) ? (esperaTotal * 1.0 / r) : 0.0;

                System.out.printf(
                        "Filósofo %d: refeições=%d, espera_total_ms=%d, tempo_medio_espera_ms=%.2f%n",
                        i, r, esperaTotal, mediaEspera
                );
            }

            System.out.println();
            System.out.println("Variação usada: simetria (quebra de simetria).");

        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo de entrada: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Execução interrompida.");
            Thread.currentThread().interrupt();
        }
    }
}
