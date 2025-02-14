package br.ufjf.tcc.controller;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import br.ufjf.tcc.business.ParticipacaoBusiness;
import br.ufjf.tcc.business.PerguntaBusiness;
import br.ufjf.tcc.business.QuestionarioBusiness;
import br.ufjf.tcc.business.RespostaBusiness;
import br.ufjf.tcc.business.TCCBusiness;
import br.ufjf.tcc.business.UsuarioBusiness;
import br.ufjf.tcc.library.FileManager;
import br.ufjf.tcc.library.SessionManager;
import br.ufjf.tcc.mail.Email;
import br.ufjf.tcc.mail.EmailBuilder;
import br.ufjf.tcc.mail.EnviadorEmailAvisoFormatacaoTrabalhoFinalReprovada;
import br.ufjf.tcc.mail.EnviadorEmailAvisoProjetoAprovado;
import br.ufjf.tcc.mail.EnviadorEmailAvisoProjetoReprovado;
import br.ufjf.tcc.mail.EnviadorEmailAvisoTrabalhoFinalAprovado;
import br.ufjf.tcc.mail.EnviadorEmailAvisoTrabalhoFinalAprovadoPorOrientador;
import br.ufjf.tcc.mail.EnviadorEmailAvisoTrabalhoFinalReprovado;
import br.ufjf.tcc.mail.EnviadorEmailAvisoTrabalhoReprovadoDefinitivo;
import br.ufjf.tcc.mail.EnviadorEmailCartaParticipacao;
import br.ufjf.tcc.mail.EnviadorEmailChain;
import br.ufjf.tcc.model.Participacao;
import br.ufjf.tcc.model.Pergunta;
import br.ufjf.tcc.model.Resposta;
import br.ufjf.tcc.model.TCC;
import br.ufjf.tcc.model.Usuario;

public class VisualizaTCCController extends CommonsController {
	private TCC tcc = null;
	private String pageTitle = "TEste";
	private boolean canAnswer = false, canDownloadFileBanca = false,
			canEdit = false;
	private List<Resposta> answers = new ArrayList<Resposta>();
	private Div informacoes, ficha;
	private boolean exibeBaixarTrabExtra;
	private boolean possuiBanca ;
	private Button btnAtualizarTCC;

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}

	@Init
	public void init(@ExecutionArgParam("id") int id, @ExecutionArgParam("btnAtualizarTCC") Button btnAtualizarTCC) {
		Integer tccId = id;
		this.btnAtualizarTCC = btnAtualizarTCC;

		if (tccId != null) {
			TCCBusiness tccBusiness = new TCCBusiness();
			tcc = tccBusiness.getTCCById(tccId);
		}
		
		if(tcc.getParticipacoes().isEmpty())
			possuiBanca = false;
		else
			possuiBanca=true;
		
		this.exibeBaixarTrabExtra = exibirBaixarTrabalhoExtra();
		
		if (tcc != null && canViewTCC()) {
			if (getUsuario() != null && checkLogin()) {
				canEdit = verificarCanEditTcc();
				if (canAnswer) {
					List<Pergunta> questions = new PerguntaBusiness()
							.getQuestionsByQuestionary(new QuestionarioBusiness()
									.getCurrentQuestionaryByCurso(tcc
											.getAluno().getCurso()));
					
					Participacao p = null;
					List<Participacao> participacoes = new ParticipacaoBusiness().getParticipacoesByUser(getUsuario());
					for (Participacao aux : participacoes) {
						if (aux.getTcc().getIdTCC() == tcc.getIdTCC())
							p = aux;
					}

					for (Pergunta question : questions) {
						Resposta answer = new Resposta();
						answer.setPergunta(question);
						answer.setParticipacao(p);
						answers.add(answer);
					}
				}

			}
		} else
			redirectHome();

	}

	private boolean canViewTCC() {
		int tipoUsuario = getUsuario().getTipoUsuario().getIdTipoUsuario();
		if (getUsuario() != null) {
			if(isSecretaria()) {
				canDownloadFileBanca = true;
				return true;
			}
			for (Participacao p : tcc.getParticipacoes())
				if (p.getProfessor().getIdUsuario() == getUsuario()
						.getIdUsuario()) {
					canDownloadFileBanca = true;
//					canAnswer = true;
					return true;
				}
			
			if (getUsuario().getIdUsuario() == tcc.getAluno().getIdUsuario()
					|| getUsuario().getIdUsuario() == tcc.getOrientador().getIdUsuario()
					|| tipoUsuario == Usuario.ADMINISTRADOR || tipoUsuario == Usuario.COORDENADOR) {
				
				canDownloadFileBanca = true;
				return true;
			}
		}

		return (tcc.getStatus() == TCC.APROVADO && tcc.getArquivoTCC() != null);
	}
	
	
	private boolean verificarCanEditTcc() {
		if(getUsuario().getCurso() == null)
			return false;
		if(getUsuario() == null || getUsuario().getCurso().getIdCurso() != tcc.getAluno().getCurso().getIdCurso()) {
			return false;
		}
		int tipoUsuario = getUsuario().getTipoUsuario().getIdTipoUsuario();
		if(tipoUsuario == Usuario.COORDENADOR || 
				tipoUsuario == Usuario.ADMINISTRADOR || 
				isSecretaria() || getUsuario().getIdUsuario() == tcc.getAluno().getIdUsuario()) {
			return true;
		}
		
		return false;
	}


	public TCC getTcc() {
		return tcc;
	}

	public void setTcc(TCC tcc) {
		this.tcc = tcc;
	}
	
	/**
	 * canAnswer decide exibir a aba de perguntas e respostas para membros da banca
	 * Ainda não implemtado.
	 * @return
	 */
	public boolean isCanAnswer() {
		return canAnswer;
	}

	public boolean isCanEdit() {
		return canEdit;
	}

	public boolean isCanDonwloadFileBanca() {
		return canDownloadFileBanca;
	}

	public List<Resposta> getAnswers() {
		return answers;
	}

	public Div getInformacoes() {
		return informacoes;
	}

	@Command
	public void setInformacoes(@BindingParam("adiv") Div informacoes) {
		this.informacoes = informacoes;
	}

	public Div getFicha() {
		return ficha;
	}

	@Command
	public void setFicha(@BindingParam("adiv") Div ficha) {
		this.ficha = ficha;
	}

	@Command
	public void showInfo() {
		ficha.setVisible(false);
		informacoes.setVisible(true);
	}

	@Command
	public void showFicha() {
		informacoes.setVisible(false);
		ficha.setVisible(true);
	}

	@Command
	public void showTCC(@BindingParam("iframe") Iframe report,@BindingParam("button") Button thisBtn,@BindingParam("button2") Button otherBtn) {
		if(otherBtn!=null)
		otherBtn.setStyle("background:white;color:black");
		if(thisBtn!=null)
		thisBtn.setStyle("background: -webkit-linear-gradient(#08c, #2E2EFE);background: -o-linear-gradient(#08c, #2E2EFE);background: -moz-linear-gradient(#08c, #2E2EFE);background: linear-gradient(#08c, #2E2EFE);color:white");
		
		InputStream is = null;
		
		if (tcc.getArquivoTCC() != null)
			is = FileManager.getFileInputSream(tcc.getArquivoTCC());
		
		if (is == null)
			is = FileManager.getFileInputSream("modelo.pdf");
		if(is != null) {
			final AMedia amedia = new AMedia(tcc.getNomeTCC() + ".pdf", "pdf",
					"application/pdf", is);
			report.setContent(amedia);
		}
	}
	
	
	@Command
	public void showTCC2(@BindingParam("iframe") Iframe report) {
		String tccId = Executions.getCurrent().getParameter("id");
		System.out.println("\n\n\n entrei");
		TCC tcc2 = null;
		if (tccId != null) {
			TCCBusiness tccBusiness = new TCCBusiness();
			tcc2 = tccBusiness.getTCCById(Integer.parseInt(tccId));
		}
		
		InputStream is;
		if (tcc2.getArquivoTCC() != null)
			is = FileManager.getFileInputSream(tcc2.getArquivoTCC());
		else
			is = FileManager.getFileInputSream("modelo.pdf");

		final AMedia amedia = new AMedia(tcc.getNomeTCC() + ".pdf", "pdf",
				"application/pdf", is);
		report.setContent(amedia);
	}

	@Command
	public void getTccYear(@BindingParam("lbl") Label lbl) {
		if (tcc.getDataEnvioFinal() != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(tcc.getDataEnvioFinal().getTime());
			lbl.setValue("" + cal.get(Calendar.YEAR));
		} else
			lbl.setValue("Não finalizada");
	}
	

	@Command
	public void downloadPDF() {
		InputStream is = FileManager
				.getFileInputSream(tcc.getArquivoTCC());
//		.getFileInputSream(tcc.getArquivoTCCFinal());
		if (is != null)
			Filedownload.save(is, "application/pdf", tcc.getNomeTCC() + ".pdf");
		else
			Messagebox.show("O PDF não foi encontrado!", "Erro", Messagebox.OK,
					Messagebox.ERROR);
	}

	@Command
	public void downloadExtra() {
		InputStream is = null;
		
		if (tcc.getArquivoExtraTCC() != null) 
			is = FileManager.getFileInputSream(tcc.getArquivoExtraTCC());
			
		if (is != null)
			Filedownload.save(is, "application/x-rar-compressed",
					tcc.getNomeTCC() + "_complemento.zip");
		else
			Messagebox.show("O ZIP não foi encontrado!", "Erro",
					Messagebox.OK, Messagebox.ERROR);
	}
	
	@Command
	public void downloadDocumentacao() {
		InputStream is = null;
		
		if (tcc.getArquivoDocumentacao() != null) 
			is = FileManager.getFileInputSream(tcc.getArquivoDocumentacao());
		
		if (is != null)
			Filedownload.save(is, "application/x-rar-compressed",
					tcc.getNomeTCC() + "_documentacao.pdf");
		else
			Messagebox.show("O PDF não foi encontrado!", "Erro",
					Messagebox.OK, Messagebox.ERROR);
	}

	@Command
	public void logout() {
		new MenuController().sair();
	}

	@Command
	public void submitFicha() {
		RespostaBusiness respostaBusiness = new RespostaBusiness();
		float sum = 0;
		for (Resposta a : answers) {
			if (respostaBusiness.validate(a)) {
				sum += a.getNota();
				if (!respostaBusiness.save(a))
					Messagebox.show("Respostas não salvas.", "Erro",
							Messagebox.OK, Messagebox.ERROR);
			} else {
				String errorMessage = "";
				for (String error : respostaBusiness.getErrors())
					errorMessage += error;
				Messagebox.show(errorMessage,
						"Dados insuficientes / inválidos", Messagebox.OK,
						Messagebox.ERROR);
				return;
			}
		}

		tcc.setConceitoFinal(sum);
		new TCCBusiness().edit(tcc);

		Messagebox.show("Conceito final: " + sum);
	}

	@Command
	public void editTCC() {
		Executions.sendRedirect("/pages/editor.zul?id=" + tcc.getIdTCC());
	}
	
	
	public boolean isCoordenador()
	{
		if(getUsuario()!=null)
		if(getUsuario().getTipoUsuario().getIdTipoUsuario()==Usuario.COORDENADOR)
			return true;
		return false;
	}
	
	public boolean isOrientador() {
		if(getUsuario().getIdUsuario() == tcc.getOrientador().getIdUsuario())
			return true;
		return false;
	}
	
	public boolean isProjeto()
	{
		return tcc.isProjeto();
	}
	
	public boolean isFinalizado()
	{
		if(tcc.getDataEnvioFinal()!=null)
			return true;
		return false;
	}
	
	@SuppressWarnings({"unchecked","rawtypes"})
	@Command
	public void aprovarProjeto(@BindingParam("window") final Window window)
	{
		Messagebox.show("Você tem certeza que deseja validar esse projeto?", "Confirmação", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
		    public void onEvent(Event evt) throws InterruptedException {
		        if (evt.getName().equals("onYes")) {
					if(isProjetoAguardandoAprovacao())
					{
						tcc.setStatus(TCC.TI);
			        	tcc.setProjeto(false);
			        	tcc.setJustificativaReprovacao(null);
						new TCCBusiness().edit(tcc);
						
						// Envio de email de aviso de projeto aprovado
						EnviadorEmailChain email = new EnviadorEmailAvisoProjetoAprovado();
						email.enviarEmail(tcc, null);
						
						SessionManager.setAttribute("trabalhos_semestre",true);
						//Executions.sendRedirect("/pages/tccs-curso.zul");
						
						if (btnAtualizarTCC != null)
							Events.sendEvent(new Event("onClick", btnAtualizarTCC));
						
						if (window != null)
							window.detach();
					}
					else
						Messagebox.show("O projeto não esta completo");
		        }
		    }
		});

	}
	
	@Command
	public void abrirModalReprovacao(@BindingParam("window") Window window)
	{
		String tipoTcc = (tcc.isProjeto() ? "Projeto" : "Trabalho");
		if(window != null) {
			window.setTitle("Solicitar correção de " + tipoTcc);
			
		}
		window.doModal();
	}
	
	@SuppressWarnings({"unchecked","rawtypes"})
	@Command
	public void reprovarDefinitivo(@BindingParam("window") final Window window)
	{
		Messagebox.show("Você tem certeza que deseja reprovar esse trabalho em definitivo? O TCC será apagado do sistema.", "Confirmação", Messagebox.YES | Messagebox.NO, Messagebox.EXCLAMATION, new org.zkoss.zk.ui.event.EventListener() {
			public void onEvent(Event evt) throws InterruptedException {
				if(evt.getName().equals("onYes")) {
					new TCCBusiness().excluirTCC(tcc);
					EnviadorEmailChain emailTrabalhoReprovado = new EnviadorEmailAvisoTrabalhoReprovadoDefinitivo();
					emailTrabalhoReprovado.enviarEmail(tcc, null);
					window.detach();
					Executions.sendRedirect("/pages/home-professor.zul");
				}
			}
		});
	}
	
	@SuppressWarnings({"unchecked","rawtypes"})
	@Command
	public void reprovar(@BindingParam("window") final Window window, @BindingParam("justificativaReprovacao") Textbox justificativaReprovacao)
	{
		String tipoTcc = (tcc.isProjeto() ? "Projeto" : "Trabalho");
		
		final String justificativa = justificativaReprovacao.getValue();
		if(justificativa != "") {
			Messagebox.show("Você tem certeza que deseja solicitar correção desse " + tipoTcc + "?", "Confirmação", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
				public void onEvent(Event evt) throws InterruptedException {
					if(evt.getName().equals("onYes")) {
						tcc.setJustificativaReprovacao(justificativa);
						int status = tcc.getStatus();
						// Envio de emails de reprovação do trabalho, solicitando correção 
						if (status == TCC.PAA) {
							tcc.setStatus(TCC.PR);
							EnviadorEmailChain emailPorjetoReprovado = new EnviadorEmailAvisoProjetoReprovado();
							emailPorjetoReprovado.enviarEmail(tcc, null);
						}
						else if(status == TCC.TAAO){
							tcc.setStatus(TCC.TRO);
							EnviadorEmailChain emailTrabalhoReprovado = new EnviadorEmailAvisoTrabalhoFinalReprovado();
							emailTrabalhoReprovado.enviarEmail(tcc, null);
						}
						else if(status == TCC.TAAC){
							tcc.setStatus(TCC.TRC);
							EnviadorEmailChain emailTrabalhoReprovado = new EnviadorEmailAvisoFormatacaoTrabalhoFinalReprovada();
							emailTrabalhoReprovado.enviarEmail(tcc, null);
						}
						new TCCBusiness().edit(tcc);
						Messagebox.show("O aluno receberá uma notificação sobre a solicitação de correção.", "Aviso", Messagebox.OK, Messagebox.INFORMATION);
						window.getParent().detach();
						
					}
				}
			});
		}
		else {
			Messagebox.show("É necessário inserir uma justificativa.", "Aviso", Messagebox.OK, Messagebox.ERROR);
		}
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void aprovarTrabalhoOrientador(@BindingParam("window") final Window window)
	{
		Messagebox.show("Você tem certeza que deseja aprovar esse Trabalho?", "Confirmação", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
		    public void onEvent(Event evt) throws InterruptedException {
		        if (evt.getName().equals("onYes")) {
		        	if(new TCCBusiness().isTrabalhoAguardandoAprovacaoDeOrientador(tcc))
		        	{
		        		tcc.setStatus(TCC.TAAC);
		        		tcc.setJustificativaReprovacao(null);
		        		new TCCBusiness().edit(tcc);
		        		
						// Email de notificação ao aluno que seu trabalho final foi aprovado
						EnviadorEmailChain email = new EnviadorEmailAvisoTrabalhoFinalAprovadoPorOrientador();
						email.enviarEmail(tcc, null);
						
						
						if (window != null)
							window.detach();
		        	}
		        	else
						Messagebox.show("O trabalho não está completo");
		        } 
		    }
		});
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void aprovarTrabalhoCoordenador(@BindingParam("window") final Window window)
	{
		Messagebox.show("Você tem certeza que deseja finalizar esse Trabalho?\nApós a aprovação, o trabalho será publicado para acesso público", "Confirmação", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
			public void onEvent(Event evt) throws InterruptedException {
				if (evt.getName().equals("onYes")) {
					java.util.Date date= new java.util.Date();
					tcc.setDataEnvioFinal(new Timestamp(date.getTime()));
					tcc.setStatus(TCC.APROVADO);
					tcc.setJustificativaReprovacao(null);
					tcc.setCertificadoDigital(gerarCertificadoDigital());
					new TCCBusiness().edit(tcc);
					
					tcc.getAluno().setAtivo(false);
					(new UsuarioBusiness()).editar(tcc.getAluno());
						
					// Email de notificação ao aluno que seu trabalho final foi aprovado
					EnviadorEmailChain email = new EnviadorEmailAvisoTrabalhoFinalAprovado();
					email.enviarEmail(tcc, null);
					
					// Email de carta para membros da banca
					EnviadorEmailCartaParticipacao emailCarta = new EnviadorEmailCartaParticipacao();
					emailCarta.enviarEmail(tcc, null);
					
					SessionManager.setAttribute("trabalhos_semestre",true);
					//Executions.sendRedirect("/pages/tccs-curso.zul");
					
					if (btnAtualizarTCC != null)
						Events.sendEvent(new Event("onClick", btnAtualizarTCC));
					
					if (window != null)
						window.detach();
				} 
			}
		});
		
	}
	
	@Command
	public void aprovarTCC(@BindingParam("window") final Window window) {
		int status = tcc.getStatus();
		if(status == TCC.PAA)
			aprovarProjeto(window);
		else if (status == TCC.TAAO)
			aprovarTrabalhoOrientador(window);
		else if (status == TCC.TAAC)
			aprovarTrabalhoCoordenador(window);
		else 
			System.out.println("N�o deveria deixar aprovar");
	}
	
	public String gerarCertificadoDigital() {
		char[] caracteres = { '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
				'A', 'B', 'D', 'E', 'F' };
		Random r = new Random();
		StringBuffer certificado = new StringBuffer();
		
		for(int i = 0; i < 5; i++) {
			for(int j = 0; j < 5; j++) {
				certificado.append(caracteres[r.nextInt(caracteres.length)]);				
			}
			if(i == 4)
				continue;
			certificado.append('-');
		}
		
		System.out.println("certificado: " + certificado.toString());
		return certificado.toString();
	}
	
	public boolean isSecretaria()
	{
		if(getUsuario()!=null)
		if(getUsuario().getTipoUsuario().getIdTipoUsuario()==Usuario.SECRETARIA)
			return true;
		return false;
	}
	
	public boolean isProjetoAguardandoAprovacao()
	{
		if(tcc!=null)
		{
			TCCBusiness tccBusiness = new TCCBusiness();
			if(tcc.getStatus() == TCC.PAA)
			{
				
				return true;
				
			}
			if(tccBusiness.isProjetoAguardandoAprovacao(tcc))
				return true;
		}
		return false;
	}
	
	
	public boolean exibirBaixarTrabalhoExtra(){
		return tcc.getArquivoExtraTCC() != null;
		
	}
	
	public boolean exibirBaixarDocumentacao() {
		if(tcc != null) {
			if(tcc.getArquivoDocumentacao() != null) {
				int tipoUsuario = getUsuario().getTipoUsuario().getIdTipoUsuario();
				if(tipoUsuario == Usuario.COORDENADOR || tipoUsuario == Usuario.SECRETARIA)
					return true;
			}
		}
		return false;
	}
	
	public boolean exibirTrabalho(){		
		if(tcc.getArquivoTCC()!=null){
			return true;
		}
		else
			return false;
	}
	
	public String getLabel(@BindingParam("botao") Button button) {
		String nome = (String) button.getAttribute("name");
		if(tcc.isProjeto())
			return nome + " projeto";
		return nome + " trabalho";
	}
	
	
	public boolean exibirAprovacao() {
		int status = tcc.getStatus();
		// PAA - coordenador aprovar projeto
		if(status == TCC.PAA && isCoordenador() && getUsuario().getCurso().getIdCurso() == tcc.getAluno().getCurso().getIdCurso()) {
			return true;
		}
		
		// TAAO - orientador aprovar trabalho
		if(status == TCC.TAAO && isOrientador()) {
			return true;
		}
		
		// TAAC - coordenação aprovar trabalho (formatação)
		if(status == TCC.TAAC && isCoordenador()  && getUsuario().getCurso().getIdCurso() == tcc.getAluno().getCurso().getIdCurso()) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Exibe botão para reprovar trabalho por definitivo 
	 */
	public boolean exibirReprovacao() {
		int status = tcc.getStatus();
		// TAAO - orientador aprovar trabalho
		if(status == TCC.TAAO && isOrientador()) {
			return true;
		}
		return false;
	}


	public boolean isExibeBaixarTrabExtra() {
		return exibeBaixarTrabExtra;
	}

	public void setExibeBaixarTrabExtra(boolean exibeBaixarTrabExtra) {
		this.exibeBaixarTrabExtra = exibeBaixarTrabExtra;
	}

	public boolean isPossuiBanca() {
		return possuiBanca;
	}

	public void setPossuiBanca(boolean possuiBanca) {
		this.possuiBanca = possuiBanca;
	}

	
}