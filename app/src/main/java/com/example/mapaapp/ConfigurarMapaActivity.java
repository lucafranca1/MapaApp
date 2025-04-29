package com.example.mapaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;

public class ConfigurarMapaActivity extends AppCompatActivity {

    private Spinner tipoMapaSpinner;
    private Spinner navegacaoSpinner;
    private RadioGroup marcadorRadioGroup;
    private Switch trafegoSwitch;
    private Button salvarConfiguracoesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configurar_mapa);

        tipoMapaSpinner = findViewById(R.id.tipo_mapa_spinner);
        navegacaoSpinner = findViewById(R.id.navegacao_spinner);
        marcadorRadioGroup = findViewById(R.id.marcador_radio_group);
        trafegoSwitch = findViewById(R.id.trafego_switch);
        salvarConfiguracoesButton = findViewById(R.id.salvar_configuracoes_button);

        // Configurar os Spinners com as opções
        ArrayAdapter<CharSequence> tipoMapaAdapter = ArrayAdapter.createFromResource(
                this, R.array.tipos_mapa, android.R.layout.simple_spinner_item);
        tipoMapaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoMapaSpinner.setAdapter(tipoMapaAdapter);

        ArrayAdapter<CharSequence> navegacaoAdapter = ArrayAdapter.createFromResource(
                this, R.array.tipos_navegacao, android.R.layout.simple_spinner_item);
        navegacaoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        navegacaoSpinner.setAdapter(navegacaoAdapter);

        salvarConfiguracoesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obter as configurações selecionadas
                String tipoMapa = tipoMapaSpinner.getSelectedItem().toString();
                String navegacao = navegacaoSpinner.getSelectedItem().toString();
                int marcadorSelecionadoId = marcadorRadioGroup.getCheckedRadioButtonId();
                RadioButton marcadorSelecionado = findViewById(marcadorSelecionadoId);
                String iconeMarcador = (marcadorSelecionado != null) ? marcadorSelecionado.getText().toString() : "Padrão";
                boolean exibirTrafego = trafegoSwitch.isChecked();

                // Criar um Intent para iniciar a VisualizarMapaActivity
                Intent intent = new Intent(ConfigurarMapaActivity.this, VisualizarMapaActivity.class);

                // Passar as configurações como extras para a próxima atividade
                intent.putExtra("tipoMapa", tipoMapa);
                intent.putExtra("navegacao", navegacao);
                intent.putExtra("iconeMarcador", iconeMarcador);
                intent.putExtra("exibirTrafego", exibirTrafego);

                startActivity(intent);
            }
        });
    }
}