package usecases.censo;

import domain.CensoIBGE;
import infrastructure.logging.Logger;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static infrastructure.logging.Logger.loggerInsercoes;

public class InserirDados {

    CensoIBGE censo = new CensoIBGE();
    Logger loggerInsercoes = Logger.getLoggerInsercoes();

    private int obterIndiceColuna(List<List<Object>> dadosExcel, String nomeColuna) {

        String cabecalho = dadosExcel.get(0).toString();
        // Remover o BOM (Byte Order Mark) da primeira célula do cabeçalho
        if (cabecalho.length() > 0 && cabecalho.charAt(0) == '\uFEFF') {
            cabecalho = cabecalho.substring(1); // Remove o Byte Order Mark
        }

        String[] colunas = cabecalho.split(",");

        for (int i = 0; i < colunas.length; i++) {
            String nomeAtual = colunas[i].trim(); // Remover espaços em branco ao redor
            if (nomeAtual.equalsIgnoreCase(nomeColuna)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Coluna '" + nomeColuna + "' não encontrada no cabeçalho.");
    }

    public void inserirDados(List<List<Object>> dadosExcel, Connection conexao) throws SQLException {

        if (conexao == null) {
            throw new SQLException("Conexão com o banco de dados não foi estabelecida.");
        }

        String query = "INSERT INTO censoIBGE (fkCidade, area, densidadeDemografica) VALUES (?, ?, ?)";
        System.out.println(query);

        try (PreparedStatement guardarValor = conexao.prepareStatement(query)) {

            System.out.println("Inserindo dados no banco...");
//            loggerInsercoes.gerarLog("💻 Iniciando inserção de dados na tabela censoIBGE... 💻");

            int indiceMunicipio = obterIndiceColuna(dadosExcel, "Município");
            int indiceDensidadeDemografica = obterIndiceColuna(dadosExcel, "Densidade demográfica(hab/km²)");
            int indiceArea = obterIndiceColuna(dadosExcel, "Área(km²)");

            for (int i = 1; i < dadosExcel.size(); i++) {
                List<Object> linha = dadosExcel.get(i);
                System.out.println(linha.toString());

                if (linha.size() >= 3 && linha.get(0) != null && linha.get(1) != null && linha.get(2) != null) {

                    censo.setCidade(linha.get(indiceMunicipio).toString().trim());
                    System.out.println(censo.getCidade());
                    censo.setArea(Double.parseDouble(linha.get(indiceArea).toString()));
                    censo.setDensidadeDemografica(Double.parseDouble(linha.get(indiceDensidadeDemografica).toString()));

                    guardarValor.setString(1, censo.getCidade());  // Cidade
                    guardarValor.setDouble(2, censo.getArea());  // Area
                    guardarValor.setDouble(3, censo.getDensidadeDemografica());  // Densidade demográfica

                    guardarValor.addBatch();  // Adicionando ao batch

                    // A cada 5000 registros, executa o batch
                    if (i % 5000 == 0) {
                        guardarValor.executeBatch();
                        conexao.commit();  // Commit manual
                    }
                }
            }

            // Executa o batch restante
            guardarValor.executeBatch();
            conexao.commit();  // Commit final
            System.out.println("Todos os dados foram inseridos.");

        } catch (SQLException e) {
            conexao.rollback();  // Reverte em caso de erro
//            loggerInsercoes.gerarLog("❌ Erro ao inserir dados em CENSO: " + e.getMessage() + " - revertendo... ❌");
            throw e;
        }
    }
}
