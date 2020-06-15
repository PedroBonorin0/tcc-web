package br.ufjf.tcc.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.ufjf.tcc.business.TCCBusiness;
import br.ufjf.tcc.library.FileManager;
import br.ufjf.tcc.model.TCC;


@WebServlet("/downloadExtra")
public class DownloadExtraServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private TCC tcc = null;
	
	public void service(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		String tccId = req.getParameter("id");

		if (tccId != null) {
			TCCBusiness tccBusiness = new TCCBusiness();
			tcc = tccBusiness.getTCCById(Integer.parseInt(tccId));
		}
		
		File file = FileManager.getFile(tcc.getArquivoExtraTCC());
		
		byte[] bytes = null;
		
		try {
			bytes = fileToByte(file);
		} catch (Exception e) {
			e.printStackTrace();
			req.getRequestDispatcher("index.jsp").forward(req, res);
			return;
		} 
		String extensaoArquivo = setExtensaoArquivo(file, res);
		
		res.setContentLength(bytes.length);
		res.setHeader("Content-Disposition",
				"attachment; filename=\""+tcc.getNomeTCC()+"."+extensaoArquivo); 
		res.getOutputStream().write(bytes);

	}
	
	private String setExtensaoArquivo(File file, HttpServletResponse res) {
		String extensaoArquivo = "rar" ;
		res.setContentType("application/x-rar-compressed");
		if(file.getName().contains("zip")){
			extensaoArquivo = "zip" ;
			res.setContentType("application/zip");
		}
		return extensaoArquivo;
	}

	private static byte[] fileToByte(File imagem) throws Exception {

		FileInputStream fis = new FileInputStream(imagem);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int bytesRead = 0;
		while ((bytesRead = fis.read(buffer, 0, 8192)) != -1) {
			baos.write(buffer, 0, bytesRead);
		}
		
		fis.close();
		return baos.toByteArray();
	}

}
